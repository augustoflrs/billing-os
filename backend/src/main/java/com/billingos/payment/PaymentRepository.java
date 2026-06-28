package com.billingos.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    @Query("""
            SELECT p FROM Payment p
            JOIN p.allocations a
            WHERE a.invoice.id = :invoiceId
            ORDER BY p.paymentDate DESC
            """)
    List<Payment> findByInvoiceId(@Param("invoiceId") String invoiceId);
}
