package com.billingos.branch;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "point_of_sale", schema = "invoicing")
@Getter
@Setter
public class PointOfSale {

    @Id
    private String id;

    @Column(name = "branch_id", nullable = false)
    private String branchId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private boolean active = true;
}
