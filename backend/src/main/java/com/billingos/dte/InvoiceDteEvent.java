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
@Table(name = "invoice_dte_event", schema = "invoicing")
@Getter
@Setter
public class InvoiceDteEvent {

    @Id
    @Column(length = 26, updatable = false)
    private String id = UlidGenerator.generate();

    @Column(name = "invoice_dte_id", nullable = false, length = 26)
    private String invoiceDteId;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_json", columnDefinition = "jsonb")
    private Map<String, Object> requestJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_json", columnDefinition = "jsonb")
    private Map<String, Object> responseJson;

    @Column(name = "event_type_code", length = 30)
    private String eventTypeCode;
}
