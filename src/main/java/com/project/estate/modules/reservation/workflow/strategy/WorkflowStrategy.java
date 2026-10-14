package com.project.estate.modules.reservation.workflow.strategy;

import com.project.estate.modules.reservation.enums.ReservationAction;
import org.aspectj.lang.JoinPoint;

/**
 * Strategy interface defining lifecycle hooks for processing specific Workflow Actions. Receives
 * AOP JoinPoint and annotation metadata to perform context initialization, state validation, and
 * persistence.
 */
public interface WorkflowStrategy {

  /** Returns the ReservationAction supported by this strategy. */
  ReservationAction getAction();

  /** Hook executed BEFORE business method invocation. */
  void beforeProcess(
      JoinPoint joinPoint, String stepName, String workflowName, String targetIdSpel);

  /** Hook executed AFTER successful business method invocation. */
  void afterProcess(JoinPoint joinPoint, String targetIdSpel, Object result);

  /** Hook executed when an EXCEPTION is thrown during business method invocation. */
  void afterThrowProcess(Throwable ex);
}
