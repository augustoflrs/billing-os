package com.billingos.invoice;

import com.billingos.common.UlidGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_line_tax", schema = "invoicing")
@Getter
@Setter
public class InvoiceLineTax {

    @Id
    private String id = UlidGenerator.generate();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_line_id", nullable = false)
    private InvoiceLine invoiceLine;

    @Column(name = "tax_code", nullable = false, length = 20)
    private String taxCode;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal rate;

    @Column(name = "taxable_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal taxableAmount;

    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal taxAmount;
}
