package com.billingos.dte;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DteTransmissionProcessor {

    private final DteTransmissionService transmissionService;

    /**
     * Every 5 seconds, picks up signed (dte_queued) DTEs whose next_attempt_at
     * is due and submits them to the MH API.
     */
    @Scheduled(fixedDelay = 5_000)
    public void transmitQueued() {
        List<InvoiceDte> ready = transmissionService.findReadyToTransmit();
        if (ready.isEmpty()) return;

        log.debug("Transmitting {} DTE(s) to MH", ready.size());
        for (InvoiceDte dte : ready) {
            try {
                transmissionService.transmit(dte);
            } catch (Exception e) {
                log.error("Unexpected error transmitting DTE {}: {}", dte.getId(), e.getMessage(), e);
            }
        }
    }
}
