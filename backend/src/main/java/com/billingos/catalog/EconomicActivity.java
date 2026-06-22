package com.billingos.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "economic_activity", schema = "invoicing")
@Getter
@NoArgsConstructor
public class EconomicActivity {

    @Id
    @Column(length = 20)
    private String code;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;
}
