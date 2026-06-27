package com.billingos.dte;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DteSigningProcessor {

    private final InvoiceDteRepository dteRepository;
    private final DteSigningService    signingService;

    /**
     * Picks up dte_draft records every 5 seconds and attempts signing.
     * Successful signing transitions to dte_queued; missing cert to dte_blocked.
     * Transient failures (MinIO unavailable, etc.) leave the record in dte_draft
     * for the next tick to retry.
     */
    @Scheduled(fixedDelay = 5_000)
    public void signDraftDtes() {
        List<InvoiceDte> drafts = dteRepository.findByCurrentStatusIdOrderByIdAsc("dte_draft");
        if (drafts.isEmpty()) return;

        log.debug("Signing {} DTE draft(s)", drafts.size());
        for (InvoiceDte dte : drafts) {
            try {
                signingService.sign(dte);
            } catch (Exception e) {
                log.error("Signing attempt failed for DTE {} (will retry): {}", dte.getId(), e.getMessage());
            }
        }
    }
}
