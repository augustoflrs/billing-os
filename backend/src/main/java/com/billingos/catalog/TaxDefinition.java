package com.billingos.catalog;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "tax_definition", schema = "invoicing")
@Getter
public class TaxDefinition {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(precision = 8, scale = 4)
    private BigDecimal rate;

    @Column(nullable = false)
    private boolean active = true;
}
