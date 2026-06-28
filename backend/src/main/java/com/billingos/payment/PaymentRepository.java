package com.billingos.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    @Query("""
            SELECT p FROM Payment p
            JOIN p.allocations a
            WHERE a.invoice.id = :invoiceId
            ORDER BY p.paymentDate DESC
            """)
    List<Payment> findByInvoiceId(@Param("invoiceId") String invoiceId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentDate >= :since")
    Optional<BigDecimal> sumAmountSince(@Param("since") Instant since);
}
