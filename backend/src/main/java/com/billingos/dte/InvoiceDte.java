package com.billingos.dte;

import com.billingos.common.UlidGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "invoice_dte", schema = "invoicing")
@Getter
@Setter
public class InvoiceDte {

    @Id
    @Column(length = 26, updatable = false)
    private String id = UlidGenerator.generate();

    @Column(name = "invoice_id", nullable = false, length = 26, unique = true)
    private String invoiceId;

    @Column(name = "generation_code", nullable = false, length = 100, unique = true)
    private String generationCode;

    @Column(name = "control_number", nullable = false, length = 100, unique = true)
    private String controlNumber;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "mh_code", length = 50)
    private String mhCode;

    @Column(name = "mh_message", columnDefinition = "text")
    private String mhMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private Map<String, Object> responsePayload;

    @Column(name = "current_status_id", length = 26)
    private String currentStatusId;
}
