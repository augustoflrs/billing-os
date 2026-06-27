package com.billingos.invoice;

import com.billingos.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    @Query("""
        SELECT i FROM Invoice i
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))
               OR i.customerId IN (
                   SELECT c.id FROM Customer c
                   WHERE LOWER(c.legalName) LIKE LOWER(CONCAT('%', :search, '%'))
               ))
        ORDER BY i.invoiceDate DESC
        """)
    Page<Invoice> search(@Param("search") String search, Pageable pageable);
}
