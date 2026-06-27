package com.billingos.dte;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DteTransmissionQueueRepository extends JpaRepository<DteTransmissionQueue, String> {
    Optional<DteTransmissionQueue> findByInvoiceDteId(String invoiceDteId);
    List<DteTransmissionQueue> findByCurrentStatusIdAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(
            String statusId, Instant now);
}
