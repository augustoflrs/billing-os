package com.billingos.common.status;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusTransitionRepository extends JpaRepository<StatusTransition, String> {

    Optional<StatusTransition> findByEntityTypeAndFromStatusIdAndToStatusIdAndActiveTrue(
            String entityType, String fromStatusId, String toStatusId);
}
