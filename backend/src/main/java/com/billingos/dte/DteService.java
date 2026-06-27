package com.billingos.dte;

import com.billingos.common.status.StatusMachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DteService {

    private final InvoiceDteRepository          dteRepository;
    private final InvoiceDteEventRepository     eventRepository;
    private final DteTransmissionQueueRepository queueRepository;
    private final StatusMachineService          statusMachine;

    @Transactional(readOnly = true)
    public DteDto.DteStatusResponse getByInvoiceId(String invoiceId) {
        InvoiceDte dte = dteRepository.findByInvoiceId(invoiceId)
                .orElse(null);
        if (dte == null) return null;

        var queue = queueRepository.findByInvoiceDteId(dte.getId());

        return new DteDto.DteStatusResponse(
                dte.getId(),
                dte.getControlNumber(),
                dte.getGenerationCode(),
                dteStatusCode(dte.getCurrentStatusId()),
                dteStatusName(dte.getCurrentStatusId()),
                dte.getMhCode(),
                dte.getMhMessage(),
                dte.getSubmittedAt(),
                dte.getAcceptedAt(),
                queue.map(DteTransmissionQueue::getAttemptCount).orElse(0),
                queue.map(DteTransmissionQueue::getNextAttemptAt).orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public List<DteDto.DteEventResponse> getEventsByInvoiceId(String invoiceId) {
        InvoiceDte dte = dteRepository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return eventRepository.findByInvoiceDteIdOrderByEventTimeAsc(dte.getId())
                .stream()
                .map(e -> new DteDto.DteEventResponse(
                        e.getId(),
                        e.getEventTypeCode(),
                        eventLabel(e.getEventTypeCode()),
                        e.getEventTime()))
                .toList();
    }

    /**
     * Manual retry: re-queues a rejected or contingency DTE for re-transmission.
     * Resets queue attempt count and clears last_error.
     */
    @Transactional
    public DteDto.DteStatusResponse retry(String invoiceId) {
        InvoiceDte dte = dteRepository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String current = dte.getCurrentStatusId();
        if (!"dte_rejected".equals(current) && !"dte_contingency".equals(current)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "DTE cannot be retried from status " + dteStatusCode(current));
        }

        statusMachine.transition("DTE", dte.getId(), current, "dte_queued", "Manual retry");
        dte.setCurrentStatusId("dte_queued");
        dteRepository.save(dte);

        queueRepository.findByInvoiceDteId(dte.getId()).ifPresentOrElse(q -> {
            q.setAttemptCount(0);
            q.setLastError(null);
            q.setNextAttemptAt(Instant.now());
            q.setCurrentStatusId("dtq_pending");
            queueRepository.save(q);
        }, () -> {
            DteTransmissionQueue q = new DteTransmissionQueue();
            q.setInvoiceDteId(dte.getId());
            q.setNextAttemptAt(Instant.now());
            queueRepository.save(q);
        });

        return getByInvoiceId(invoiceId);
    }

    // ─── static mappings ─────────────────────────────────────────────────────

    private String dteStatusCode(String id) {
        if (id == null) return null;
        return switch (id) {
            case "dte_draft"       -> "DRAFT";
            case "dte_queued"      -> "QUEUED";
            case "dte_submitted"   -> "SUBMITTED";
            case "dte_accepted"    -> "ACCEPTED";
            case "dte_rejected"    -> "REJECTED";
            case "dte_contingency" -> "CONTINGENCY";
            case "dte_blocked"     -> "BLOCKED";
            default                -> id;
        };
    }

    private String dteStatusName(String id) {
        if (id == null) return null;
        return switch (id) {
            case "dte_draft"       -> "Generando";
            case "dte_queued"      -> "En Cola";
            case "dte_submitted"   -> "Enviado";
            case "dte_accepted"    -> "Aceptado";
            case "dte_rejected"    -> "Rechazado";
            case "dte_contingency" -> "Contingencia";
            case "dte_blocked"     -> "Bloqueado";
            default                -> id;
        };
    }

    private String eventLabel(String code) {
        if (code == null) return "";
        return switch (code) {
            case "SUBMISSION"         -> "Enviado a MH";
            case "ACCEPTANCE"         -> "Aceptado por MH";
            case "REJECTION"          -> "Rechazado por MH";
            case "RETRY"              -> "Reintento programado";
            case "CONTINGENCY_ENTRY"  -> "Entró en contingencia";
            case "CONTINGENCY_EXIT"   -> "Salió de contingencia";
            case "INVALIDATION"       -> "Anulación enviada";
            case "INVALIDATION_ACCEPTED" -> "Anulación aceptada";
            default                   -> code;
        };
    }
}
