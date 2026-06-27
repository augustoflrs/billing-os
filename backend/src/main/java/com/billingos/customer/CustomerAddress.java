package com.billingos.customer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer_address", schema = "invoicing")
@Getter
@Setter
public class CustomerAddress {

    @Id
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "department_code", length = 10)
    private String departmentCode;

    @Column(name = "municipality_code", length = 10)
    private String municipalityCode;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = true;
}
