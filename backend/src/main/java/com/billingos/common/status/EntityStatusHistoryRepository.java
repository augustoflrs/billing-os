package com.billingos.common.status;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntityStatusHistoryRepository extends JpaRepository<EntityStatusHistory, String> {

    List<EntityStatusHistory> findByEntityTypeAndEntityIdOrderByChangedAtAsc(
            String entityType, String entityId);
}
