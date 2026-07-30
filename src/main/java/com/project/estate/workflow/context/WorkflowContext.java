package com.project.estate.workflow.context;

import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.ReservationActor;
import com.project.estate.enums.ReservationStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

/**
 * Context holding runtime execution data of the current Workflow in ThreadLocal.
 * Must ALWAYS be cleared in a finally block to prevent ThreadLocal memory leaks in Thread Pools.
 */
@Data
@Builder
public class WorkflowContext {

    private static final ThreadLocal<WorkflowContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private String workflowInstanceId;
    private String workflowName;
    private String processRequestId;
    private String targetId;
    private ReservationAction action;
    private String currentStep;
    private ReservationStatus previousStatus;
    private ReservationStatus newStatus;
    private String userId;
    private ReservationActor actor;

    public static void set(WorkflowContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static WorkflowContext get() {
        return CONTEXT_HOLDER.get();
    }

    public static Optional<WorkflowContext> getOptional() {
        return Optional.ofNullable(CONTEXT_HOLDER.get());
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
