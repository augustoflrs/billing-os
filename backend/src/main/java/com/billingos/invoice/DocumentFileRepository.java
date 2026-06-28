package com.billingos.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentFileRepository extends JpaRepository<DocumentFile, String> {
    Optional<DocumentFile> findTopByInvoiceIdAndFileTypeOrderByCreatedAtDesc(String invoiceId, String fileType);
}
