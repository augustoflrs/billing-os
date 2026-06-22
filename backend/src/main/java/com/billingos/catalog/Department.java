package com.billingos.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "catalog_department", schema = "invoicing")
@Getter
@Setter
public class Department {

    @Id
    private String code;

    @Column(nullable = false, length = 100)
    private String name;
}
