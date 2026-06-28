package com.billingos.invoice;

import com.billingos.common.UlidGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoice", schema = "invoicing")
@Getter
@Setter
public class Invoice {

    @Id
    private String id = UlidGenerator.generate();

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "customer_id", nullable = false, length = 26)
    private String customerId;

    @Column(name = "point_of_sale_id", nullable = false, length = 26)
    private String pointOfSaleId;

    @Column(name = "document_type_code", nullable = false, length = 10)
    private String documentTypeCode;

    @Column(name = "invoice_date", nullable = false)
    private Instant invoiceDate;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    @Column(name = "current_status_id", length = 26)
    private String currentStatusId;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<InvoiceLine> lines = new ArrayList<>();
}
