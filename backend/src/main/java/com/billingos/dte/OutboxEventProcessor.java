package com.billingos.dte;

import com.billingos.common.outbox.OutboxEvent;
import com.billingos.common.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessor {

    private final OutboxEventRepository      outboxRepository;
    private final DtePayloadBuilderService   dteBuilder;

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    public void processPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        if (pending.isEmpty()) return;

        log.debug("Processing {} pending outbox events", pending.size());
        for (OutboxEvent event : pending) {
            if (!"INVOICE_CONFIRMED".equals(event.getEventType())) continue;
            try {
                String invoiceId = (String) event.getPayload().get("invoiceId");
                if (invoiceId == null) {
                    log.warn("INVOICE_CONFIRMED outbox event {} missing invoiceId payload", event.getId());
                    event.setStatus("FAILED");
                } else {
                    dteBuilder.buildAndPersist(invoiceId);
                    event.setStatus("PROCESSED");
                    event.setProcessedAt(Instant.now());
                }
            } catch (Exception e) {
                log.error("Failed to process outbox event {}: {}", event.getId(), e.getMessage(), e);
                event.setStatus("FAILED");
            }
            outboxRepository.save(event);
        }
    }
}
