package com.project.estate.workflow.strategy;

import com.project.estate.enums.ReservationActor;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.enums.WorkflowHistoryStatus;
import com.project.estate.workflow.context.WorkflowContext;
import com.project.estate.workflow.service.WorkflowPersistenceService;
import com.project.estate.workflow.statemachine.ReservationStateMachine;
import com.project.estate.workflow.util.SpelEvaluator;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Clean Abstract Template Class defining the standard lifecycle execution for Workflow Strategies.
 * Delegates SpEL evaluation to SpelEvaluator and DB persistence to WorkflowPersistenceService.
 */
@Slf4j
@Component
public abstract class AbstractWorkflowStrategy implements WorkflowStrategy {

  @Autowired protected WorkflowPersistenceService persistenceService;

  @Autowired protected SpelEvaluator spelEvaluator;

  @Override
  public void beforeProcess(
      JoinPoint joinPoint, String stepName, String workflowName, String targetIdSpel) {
    String requestId = UUID.randomUUID().toString().substring(0, 8);
    String targetId = spelEvaluator.parseTargetId(joinPoint, targetIdSpel, null);

    var existingInstance = persistenceService.findInstance(workflowName, targetId).orElse(null);
    ReservationStatus previousStatus =
        existingInstance != null ? mapToReservationStatus(existingInstance.getStatus()) : null;

    log.info(
        "[WORKFLOW_STRATEGY] Pre-processing action={} for targetId={}, currentStatus={}",
        getAction(),
        targetId,
        previousStatus);

    WorkflowContext existingCtx = WorkflowContext.get();
    ReservationActor actor =
        (existingCtx != null && existingCtx.getActor() != null)
            ? existingCtx.getActor()
            : getDefaultActor();

    // Validate state transition & resolve next status via State Machine
    ReservationStatus nextStatus =
        ReservationStateMachine.getNextState(previousStatus, getAction(), actor);

    WorkflowContext contextToSet =
        WorkflowContext.builder()
            .workflowInstanceId(existingInstance != null ? existingInstance.getId() : null)
            .workflowName(workflowName)
            .currentStep(stepName)
            .action(getAction())
            .targetId(targetId)
            .processRequestId(requestId)
            .previousStatus(previousStatus)
            .newStatus(nextStatus)
            .actor(actor)
            .userId("system-user")
            .build();

    WorkflowContext.set(contextToSet);
    doBeforeProcess(contextToSet);
  }

  @Override
  public void afterProcess(JoinPoint joinPoint, String targetIdSpel, Object result) {
    WorkflowContext context = WorkflowContext.get();
    if (context == null) return;

    // Extract targetId post-execution if needed (e.g. #result.id) via SpEL
    if (context.getTargetId() == null || context.getTargetId().isBlank()) {
      String extractedId = spelEvaluator.parseTargetId(joinPoint, targetIdSpel, result);
      if (extractedId != null && !extractedId.isBlank()) {
        context.setTargetId(extractedId);
      } else if (result != null) {
        context.setTargetId(result.toString());
      }
    }

    log.info(
        "[WORKFLOW_STRATEGY] Post-processing action={} for targetId={}, newStatus={}",
        getAction(),
        context.getTargetId(),
        context.getNewStatus());

    persistenceService.saveHistory(context, WorkflowHistoryStatus.SUCCESS, null);
    doAfterProcess(context);
  }

  @Override
  public void afterThrowProcess(Throwable ex) {
    WorkflowContext context = WorkflowContext.get();
    if (context == null) return;

    log.error(
        "[WORKFLOW_STRATEGY] Error-processing action={} for targetId={}, error={}",
        getAction(),
        context.getTargetId(),
        ex.getMessage());

    persistenceService.saveHistory(context, WorkflowHistoryStatus.FAILED, ex.getMessage());
    doAfterThrowProcess(context, ex);
  }

  protected ReservationActor getDefaultActor() {
    return ReservationActor.CUSTOMER;
  }

  protected void doBeforeProcess(WorkflowContext context) {}

  protected void doAfterProcess(WorkflowContext context) {}

  protected void doAfterThrowProcess(WorkflowContext context, Throwable ex) {}

  private ReservationStatus mapToReservationStatus(
      com.project.estate.enums.WorkflowInstanceStatus status) {
    if (status == null) return null;
    return switch (status) {
      case IN_PROGRESS -> ReservationStatus.ACTIVE;
      case COMPLETED -> ReservationStatus.COMPLETED;
      case CANCELLED -> ReservationStatus.CANCELLED;
      case EXPIRED -> ReservationStatus.EXPIRED;
      case DEPOSIT_PAID -> ReservationStatus.DEPOSIT_PAID;
      default -> null;
    };
  }
}
