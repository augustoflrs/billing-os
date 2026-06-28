package com.billingos.payment;

import com.billingos.common.UlidGenerator;
import com.billingos.invoice.Invoice;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_allocation", schema = "invoicing")
@Getter
@Setter
public class PaymentAllocation {

    @Id
    @Column(length = 26, updatable = false)
    private String id = UlidGenerator.generate();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "allocated_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal allocatedAmount;
}
