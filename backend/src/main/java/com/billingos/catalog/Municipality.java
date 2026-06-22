package com.billingos.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "catalog_municipality", schema = "invoicing")
@Getter
@Setter
public class Municipality {

    @Id
    private String code;

    @Column(name = "department_code", nullable = false, length = 10)
    private String departmentCode;

    @Column(nullable = false, length = 100)
    private String name;
}
