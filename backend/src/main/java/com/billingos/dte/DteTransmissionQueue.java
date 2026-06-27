package com.billingos.dte;

import com.billingos.common.UlidGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "dte_transmission_queue", schema = "invoicing")
@Getter
@Setter
public class DteTransmissionQueue {

    @Id
    @Column(length = 26, updatable = false)
    private String id = UlidGenerator.generate();

    @Column(name = "invoice_dte_id", nullable = false, length = 26, unique = true)
    private String invoiceDteId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "current_status_id", length = 26)
    private String currentStatusId = "dtq_pending";
}
