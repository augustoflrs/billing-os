package com.billingos.item;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "item_price", schema = "invoicing")
@Getter
@Setter
public class ItemPrice {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billable_item_id", nullable = false)
    private BillableItem billableItem;

    @Column(name = "currency_code", nullable = false, columnDefinition = "char(3)")
    private String currencyCode = "USD";

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_to")
    private OffsetDateTime validTo;

    @Column(nullable = false)
    private boolean active = true;
}
