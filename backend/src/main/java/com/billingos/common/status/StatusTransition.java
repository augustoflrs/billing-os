package com.billingos.common.status;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "status_transition", schema = "invoicing")
@Getter
public class StatusTransition {

    @Id
    private String id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "from_status_id", nullable = false, length = 26)
    private String fromStatusId;

    @Column(name = "to_status_id", nullable = false, length = 26)
    private String toStatusId;

    @Column(nullable = false)
    private boolean active = true;
}
