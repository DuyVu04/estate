package com.project.estate.workflow.service;

import com.project.estate.entity.WorkflowHistory;
import com.project.estate.entity.WorkflowInstance;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.enums.WorkflowHistoryStatus;
import com.project.estate.enums.WorkflowInstanceStatus;
import com.project.estate.repository.WorkflowHistoryRepository;
import com.project.estate.repository.WorkflowInstanceRepository;
import com.project.estate.workflow.context.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Component encapsulating database persistence operations for Workflow Instances and Audit Histories.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowPersistenceService {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowHistoryRepository historyRepository;

    public Optional<WorkflowInstance> findInstance(String workflowName, String targetId) {
        if (targetId == null || targetId.isBlank()) {
            return Optional.empty();
        }
        return instanceRepository.findByWorkflowNameAndTargetId(workflowName, targetId);
    }

    public ReservationStatus findPreviousStatus(String workflowName, String targetId) {
        return findInstance(workflowName, targetId)
                .map(inst -> mapToReservationStatus(inst.getStatus()))
                .orElse(null);
    }

    public WorkflowInstance saveOrUpdateInstance(WorkflowContext context) {
        if (context.getTargetId() == null || context.getTargetId().isBlank()) {
            return null;
        }

        WorkflowInstance instance = findInstance(context.getWorkflowName(), context.getTargetId())
                .orElseGet(() -> WorkflowInstance.builder()
                        .workflowName(context.getWorkflowName())
                        .targetId(context.getTargetId())
                        .build());

        instance.setCurrentStep(context.getCurrentStep());
        instance.setStatus(mapToWorkflowInstanceStatus(context.getNewStatus()));
        return instanceRepository.save(instance);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveHistory(WorkflowContext context, WorkflowHistoryStatus status, String errorMessage) {
        if (context.getTargetId() == null || context.getTargetId().isBlank()) {
            return;
        }

        WorkflowInstance instance = saveOrUpdateInstance(context);
        if (instance == null) {
            return;
        }
        context.setWorkflowInstanceId(instance.getId());

        WorkflowHistory history = WorkflowHistory.builder()
                .workflowInstanceId(instance.getId())
                .action(context.getAction().name())
                .step(context.getCurrentStep())
                .previousStatus(context.getPreviousStatus() != null ? context.getPreviousStatus().name() : "NONE")
                .newStatus(context.getNewStatus() != null ? context.getNewStatus().name() : "NONE")
                .performedBy(context.getUserId())
                .status(status)
                .errorMessage(errorMessage)
                .build();

        historyRepository.save(history);
    }

    private ReservationStatus mapToReservationStatus(WorkflowInstanceStatus status) {
        if (status == null) return null;
        return switch (status) {
            case IN_PROGRESS -> ReservationStatus.ACTIVE;
            case COMPLETED -> ReservationStatus.COMPLETED;
            case CANCELLED -> ReservationStatus.CANCELLED;
            case EXPIRED -> ReservationStatus.EXPIRED;
            default -> null;
        };
    }

    private WorkflowInstanceStatus mapToWorkflowInstanceStatus(ReservationStatus status) {
        if (status == null) return WorkflowInstanceStatus.IN_PROGRESS;
        return switch (status) {
            case ACTIVE -> WorkflowInstanceStatus.IN_PROGRESS;
            case COMPLETED -> WorkflowInstanceStatus.COMPLETED;
            case CANCELLED -> WorkflowInstanceStatus.CANCELLED;
            case EXPIRED -> WorkflowInstanceStatus.EXPIRED;
        };
    }
}
