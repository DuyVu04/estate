package com.project.estate.workflow.strategy;

import com.project.estate.enums.ReservationAction;
import com.project.estate.workflow.context.WorkflowContext;

/**
 * Strategy interface defining lifecycle hooks for processing specific Workflow Actions.
 */
public interface WorkflowStrategy {

    /**
     * Returns the ReservationAction supported by this strategy.
     */
    ReservationAction getAction();

    /**
     * Hook executed BEFORE business method invocation.
     */
    void beforeProcess(WorkflowContext context);

    /**
     * Hook executed AFTER successful business method invocation.
     */
    void afterProcess(WorkflowContext context);

    /**
     * Hook executed when an EXCEPTION is thrown during business method invocation.
     */
    void afterThrowProcess(WorkflowContext context, Throwable ex);
}
