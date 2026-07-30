package com.project.estate.workflow.annotation;

import com.project.estate.enums.ReservationAction;

import java.lang.annotation.*;

/**
 * Custom annotation to mark a method as a Workflow Step execution.
 * Contains metadata interpreted at runtime by WorkflowAspect.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WorkflowEngine {

    /**
     * The action being performed in this workflow step (e.g., CREATE, CANCEL, COMPLETE, EXPIRE).
     */
    ReservationAction action();

    /**
     * Name of the specific step (e.g., "create-reservation", "cancel-reservation").
     */
    String step() default "";

    /**
     * Name of the workflow definition (defaults to "reservation-workflow").
     */
    String workflowName() default "reservation-workflow";

    /**
     * Optional SpEL expression to extract target entity ID from method parameter or return value.
     * Examples: "#result.id", "#id", "#request.reservationId"
     */
    String targetIdSpel() default "";
}
