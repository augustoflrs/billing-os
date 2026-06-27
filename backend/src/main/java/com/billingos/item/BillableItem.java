package com.billingos.item;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "billable_item", schema = "invoicing")
@Getter
@Setter
public class BillableItem {

    @Id
    private String id;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    @Column(length = 100)
    private String sku;

    @Column(length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    private Long version;

    @OneToMany(mappedBy = "billableItem", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("validFrom DESC")
    private List<ItemPrice> prices = new ArrayList<>();
}
