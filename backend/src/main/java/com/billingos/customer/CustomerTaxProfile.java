package com.billingos.customer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer_tax_profile", schema = "invoicing")
@Getter
@Setter
public class CustomerTaxProfile {

    @Id
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "document_type", nullable = false, length = 20)
    private String documentType;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(length = 20)
    private String nit;

    @Column(length = 20)
    private String nrc;

    @Column(name = "economic_activity_code", length = 20)
    private String economicActivityCode;
}
