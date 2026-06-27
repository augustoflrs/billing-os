package com.billingos.dte;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceDteEventRepository extends JpaRepository<InvoiceDteEvent, String> {
    List<InvoiceDteEvent> findByInvoiceDteIdOrderByEventTimeAsc(String invoiceDteId);
}
