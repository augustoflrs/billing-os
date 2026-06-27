package com.billingos.common.status;

import com.billingos.common.audit.AuditLog;
import com.billingos.common.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatusMachineService {

    private final StatusTransitionRepository transitionRepository;
    private final EntityStatusHistoryRepository historyRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * Validates the transition against status_transition table, writes history and audit.
     * Throws IllegalStateException (→ 422) if transition is not permitted.
     */
    public void transition(String entityType, String entityId,
                           String fromStatusId, String toStatusId,
                           String reason) {
        transitionRepository.findByEntityTypeAndFromStatusIdAndToStatusIdAndActiveTrue(
                        entityType, fromStatusId, toStatusId)
                .orElseThrow(() -> new IllegalStateException(
                        "Transition from %s to %s is not permitted for %s"
                                .formatted(fromStatusId, toStatusId, entityType)));

        String actor = currentUser();

        EntityStatusHistory history = new EntityStatusHistory();
        history.setEntityType(entityType);
        history.setEntityId(entityId);
        history.setOldStatusId(fromStatusId);
        history.setNewStatusId(toStatusId);
        history.setChangedAt(Instant.now());
        history.setChangedBy(actor);
        history.setReason(reason);
        historyRepository.save(history);

        AuditLog audit = new AuditLog();
        audit.setEntityName(entityType);
        audit.setEntityId(entityId);
        audit.setOperation("STATUS_CHANGE");
        audit.setOldValues(Map.of("statusId", fromStatusId));
        audit.setNewValues(Map.of("statusId", toStatusId));
        audit.setChangedBy(actor);
        audit.setChangedAt(Instant.now());
        auditLogRepository.save(audit);
    }

    private String currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
