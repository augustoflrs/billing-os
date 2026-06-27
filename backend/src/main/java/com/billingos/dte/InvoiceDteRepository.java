package com.billingos.dte;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceDteRepository extends JpaRepository<InvoiceDte, String> {
    Optional<InvoiceDte> findByInvoiceId(String invoiceId);
    boolean existsByInvoiceId(String invoiceId);
    List<InvoiceDte> findByCurrentStatusIdOrderByIdAsc(String statusId);
}
