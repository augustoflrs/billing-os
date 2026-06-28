package com.billingos.invoice;

import com.billingos.common.UlidGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "document_file", schema = "invoicing")
@Getter
@Setter
public class DocumentFile {

    @Id
    @Column(length = 26, updatable = false)
    private String id = UlidGenerator.generate();

    @Column(name = "invoice_id", length = 26)
    private String invoiceId;

    @Column(name = "invoice_dte_id", length = 26)
    private String invoiceDteId;

    @Column(name = "file_type", nullable = false, length = 30)
    private String fileType;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
