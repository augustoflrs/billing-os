package com.billingos.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    @Query("""
        SELECT i FROM Invoice i
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))
               OR i.customerId IN (
                   SELECT c.id FROM Customer c
                   WHERE LOWER(c.legalName) LIKE LOWER(CONCAT('%', :search, '%'))
               ))
          AND (:statusId IS NULL OR :statusId = '' OR i.currentStatusId = :statusId)
          AND (:customerId IS NULL OR :customerId = '' OR i.customerId = :customerId)
          AND i.invoiceDate >= :from
          AND i.invoiceDate < :to
        ORDER BY i.invoiceDate DESC
        """)
    Page<Invoice> search(
            @Param("search")     String search,
            @Param("statusId")   String statusId,
            @Param("customerId") String customerId,
            @Param("from")       Instant from,
            @Param("to")         Instant to,
            Pageable pageable);
}
