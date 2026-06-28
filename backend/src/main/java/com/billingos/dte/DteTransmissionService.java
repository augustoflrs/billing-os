package com.billingos.dte;

import com.billingos.common.status.StatusMachineService;
import com.billingos.company.CompanyRepository;
import com.billingos.config.AppProperties;
import com.billingos.invoice.InvoicePdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DteTransmissionService {

    private static final String ENTITY_TYPE       = "DTE";
    private static final String S_QUEUED          = "dte_queued";
    private static final String S_SUBMITTED       = "dte_submitted";
    private static final String S_ACCEPTED        = "dte_accepted";
    private static final String S_REJECTED        = "dte_rejected";
    private static final String S_CONTINGENCY     = "dte_contingency";

    private final InvoiceDteRepository          dteRepository;
    private final DteTransmissionQueueRepository queueRepository;
    private final InvoiceDteEventRepository     eventRepository;
    private final MhApiClient                   mhApiClient;
    private final StatusMachineService          statusMachine;
    private final CompanyRepository             companyRepository;
    private final AppProperties                 appProperties;
    private final InvoicePdfService             pdfService;

    /**
     * Returns all dte_queued DTEs whose queue entry is ready for a transmission attempt
     * (next_attempt_at <= now, or no queue entry yet).
     */
    @Transactional(readOnly = true)
    public List<InvoiceDte> findReadyToTransmit() {
        List<InvoiceDte> queued = dteRepository.findByCurrentStatusIdOrderByIdAsc(S_QUEUED);
        Instant now = Instant.now();
        return queued.stream().filter(dte -> {
            return queueRepository.findByInvoiceDteId(dte.getId())
                    .map(q -> !q.getCurrentStatusId().equals("dtq_sent") &&
                              !q.getCurrentStatusId().equals("dtq_failed") &&
                              (q.getNextAttemptAt() == null || !q.getNextAttemptAt().isAfter(now)))
                    .orElse(true); // no queue entry yet → ready
        }).toList();
    }

    /**
     * Attempts to transmit one DTE to MH. Handles ACCEPTED, REJECTED, and transient errors
     * with exponential backoff. All outcomes write an invoice_dte_event row.
     */
    @Transactional
    public void transmit(InvoiceDte dte) {
        String companyNit = companyRepository.findAll().stream()
                .findFirst().map(c -> c.getNit())
                .orElseThrow(() -> new IllegalStateException("No company configured"));

        int maxRetries = appProperties.getDte().getMaxRetryAttempts();
        int baseDelay  = appProperties.getDte().getRetryBaseDelaySeconds();

        // Ensure queue record exists
        DteTransmissionQueue queue = queueRepository.findByInvoiceDteId(dte.getId())
                .orElseGet(() -> {
                    DteTransmissionQueue q = new DteTransmissionQueue();
                    q.setInvoiceDteId(dte.getId());
                    q.setNextAttemptAt(Instant.now());
                    return queueRepository.save(q);
                });

        // Transition DTE: dte_queued → dte_submitted
        statusMachine.transition(ENTITY_TYPE, dte.getId(), S_QUEUED, S_SUBMITTED,
                "Attempt #" + (queue.getAttemptCount() + 1));
        dte.setCurrentStatusId(S_SUBMITTED);
        dteRepository.save(dte);

        queue.setAttemptCount(queue.getAttemptCount() + 1);

        // Submit to MH
        MhResponse response = mhApiClient.submit(dte.getRequestPayload(), companyNit);

        switch (response.kind()) {
            case ACCEPTED -> handleAccepted(dte, queue, response);
            case REJECTED -> handleRejected(dte, queue, response);
            case TRANSIENT_ERROR -> handleTransientError(dte, queue, response, maxRetries, baseDelay);
        }

        queueRepository.save(queue);
    }

    // ─── private ─────────────────────────────────────────────────────────────

    private void handleAccepted(InvoiceDte dte, DteTransmissionQueue queue, MhResponse r) {
        statusMachine.transition(ENTITY_TYPE, dte.getId(), S_SUBMITTED, S_ACCEPTED,
                "MH accepted: " + r.mhCode());
        dte.setCurrentStatusId(S_ACCEPTED);
        dte.setAcceptedAt(Instant.now());
        dte.setMhCode(r.mhCode());
        dte.setMhMessage(r.mhMessage());
        dteRepository.save(dte);

        queue.setCurrentStatusId("dtq_sent");
        writeEvent(dte.getId(), "ACCEPTANCE", r.requestJson(), r.responseJson());

        log.info("DTE {} ACCEPTED by MH — sello={}", dte.getId(), r.selloRecibido());

        try {
            pdfService.generateAndStore(dte.getInvoiceId());
        } catch (Exception e) {
            log.error("PDF generation failed for invoice {} — non-fatal: {}", dte.getInvoiceId(), e.getMessage(), e);
        }
    }

    private void handleRejected(InvoiceDte dte, DteTransmissionQueue queue, MhResponse r) {
        statusMachine.transition(ENTITY_TYPE, dte.getId(), S_SUBMITTED, S_REJECTED,
                "MH rejected: " + r.mhCode() + " " + r.mhMessage());
        dte.setCurrentStatusId(S_REJECTED);
        dte.setMhCode(r.mhCode());
        dte.setMhMessage(r.mhMessage());
        dteRepository.save(dte);

        queue.setCurrentStatusId("dtq_failed");
        writeEvent(dte.getId(), "REJECTION", r.requestJson(), r.responseJson());

        log.warn("DTE {} REJECTED by MH — code={} msg={}", dte.getId(), r.mhCode(), r.mhMessage());
    }

    private void handleTransientError(InvoiceDte dte, DteTransmissionQueue queue,
                                      MhResponse r, int maxRetries, int baseDelay) {
        queue.setLastError(r.mhMessage());

        if (queue.getAttemptCount() >= maxRetries) {
            statusMachine.transition(ENTITY_TYPE, dte.getId(), S_SUBMITTED, S_CONTINGENCY,
                    "Max retries (" + maxRetries + ") exceeded: " + r.mhMessage());
            dte.setCurrentStatusId(S_CONTINGENCY);
            dteRepository.save(dte);

            queue.setCurrentStatusId("dtq_failed");
            writeEvent(dte.getId(), "CONTINGENCY_ENTRY", r.requestJson(), null);

            log.error("DTE {} entered CONTINGENCY after {} attempts: {}",
                    dte.getId(), queue.getAttemptCount(), r.mhMessage());
        } else {
            // Back to dte_queued for retry
            statusMachine.transition(ENTITY_TYPE, dte.getId(), S_SUBMITTED, S_QUEUED,
                    "Retry scheduled: " + r.mhMessage());
            dte.setCurrentStatusId(S_QUEUED);
            dteRepository.save(dte);

            // Exponential backoff: base * 2^(attempt-1), capped at 1h
            long delaySeconds = Math.min((long) baseDelay * (1L << (queue.getAttemptCount() - 1)), 3600);
            queue.setNextAttemptAt(Instant.now().plusSeconds(delaySeconds));
            writeEvent(dte.getId(), "RETRY", r.requestJson(), null);

            log.warn("DTE {} retry #{} scheduled in {}s: {}",
                    dte.getId(), queue.getAttemptCount(), delaySeconds, r.mhMessage());
        }
    }

    private void writeEvent(String dteId, String eventTypeCode,
                             java.util.Map<String, Object> req,
                             java.util.Map<String, Object> res) {
        InvoiceDteEvent event = new InvoiceDteEvent();
        event.setInvoiceDteId(dteId);
        event.setEventTypeCode(eventTypeCode);
        event.setRequestJson(req);
        event.setResponseJson(res);
        eventRepository.save(event);
    }
}
