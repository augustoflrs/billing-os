package com.billingos.payment;

import com.billingos.common.UlidGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment", schema = "invoicing")
@Getter
@Setter
public class Payment {

    @Id
    @Column(length = 26, updatable = false)
    private String id = UlidGenerator.generate();

    @Column(name = "payment_date", nullable = false)
    private Instant paymentDate;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "payment_method_code", length = 20)
    private String paymentMethodCode;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "current_status_id", length = 26)
    private String currentStatusId = "pay_confirmed";

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentAllocation> allocations = new ArrayList<>();
}
