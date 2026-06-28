package com.billingos.invoice;

import com.billingos.common.status.StatusMachineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Hourly job that transitions inv_issued / inv_partial invoices whose due_date
 * has passed to inv_overdue.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueInvoiceProcessor {

    private static final String ENTITY_TYPE  = "INVOICE";
    private static final String S_ISSUED     = "inv_issued";
    private static final String S_PARTIAL    = "inv_partial";
    private static final String S_OVERDUE    = "inv_overdue";

    private final InvoiceRepository  invoiceRepository;
    private final StatusMachineService statusMachine;

    @Scheduled(fixedDelay = 3_600_000) // every hour
    @Transactional
    public void markOverdue() {
        List<Invoice> overdue = invoiceRepository.findOverdue(Instant.now());
        if (overdue.isEmpty()) return;

        log.info("Marking {} invoice(s) as overdue", overdue.size());
        for (Invoice invoice : overdue) {
            try {
                statusMachine.transition(ENTITY_TYPE, invoice.getId(),
                        invoice.getCurrentStatusId(), S_OVERDUE,
                        "Due date passed: " + invoice.getDueDate());
                invoice.setCurrentStatusId(S_OVERDUE);
                invoiceRepository.save(invoice);
            } catch (Exception e) {
                log.error("Failed to mark invoice {} as overdue: {}", invoice.getId(), e.getMessage());
            }
        }
    }
}
