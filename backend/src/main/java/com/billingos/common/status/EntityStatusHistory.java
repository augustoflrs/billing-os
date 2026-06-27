package com.billingos.common.status;

import com.billingos.common.UlidGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "entity_status_history", schema = "invoicing")
@Getter
@Setter
public class EntityStatusHistory {

    @Id
    private String id = UlidGenerator.generate();

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 26)
    private String entityId;

    @Column(name = "old_status_id", length = 26)
    private String oldStatusId;

    @Column(name = "new_status_id", nullable = false, length = 26)
    private String newStatusId;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(columnDefinition = "text")
    private String reason;
}
