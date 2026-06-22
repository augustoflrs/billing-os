package com.billingos.branch;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "company_branch", schema = "invoicing")
@Getter
@Setter
public class Branch {

    @Id
    private String id;

    @Column(name = "company_id", nullable = false)
    private String companyId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "department_code", length = 10)
    private String departmentCode;

    @Column(name = "municipality_code", length = 10)
    private String municipalityCode;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;
}
