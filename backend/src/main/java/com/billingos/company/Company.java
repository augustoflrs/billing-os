package com.billingos.company;

import com.billingos.catalog.EconomicActivity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "company", schema = "invoicing")
@Getter
@Setter
@NoArgsConstructor
public class Company {

    @Id
    @Column(length = 26, updatable = false, nullable = false)
    private String id;

    @Column(name = "legal_name", length = 255, nullable = false)
    private String legalName;

    @Column(name = "trade_name", length = 255)
    private String tradeName;

    @Column(length = 20, nullable = false, unique = true)
    private String nit;

    @Column(length = 20)
    private String nrc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "economic_activity_code")
    private EconomicActivity economicActivity;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 100, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
