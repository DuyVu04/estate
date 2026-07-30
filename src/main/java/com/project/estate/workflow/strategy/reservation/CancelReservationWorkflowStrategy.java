package com.project.estate.workflow.strategy.reservation;

import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.workflow.context.WorkflowContext;
import com.project.estate.workflow.statemachine.ReservationStateMachine;
import com.project.estate.workflow.strategy.WorkflowStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CancelReservationWorkflowStrategy implements WorkflowStrategy {

    @Override
    public ReservationAction getAction() {
        return ReservationAction.CANCEL;
    }

    @Override
    public void beforeProcess(WorkflowContext context) {
        log.info("[CANCEL_WORKFLOW] Validating CANCEL reservation for targetId={}, currentStatus={}",
                context.getTargetId(), context.getPreviousStatus());
        ReservationStatus nextStatus = ReservationStateMachine.getNextState(
                context.getPreviousStatus(), getAction(), context.getActor()
        );
        context.setNewStatus(nextStatus);
    }

    @Override
    public void afterProcess(WorkflowContext context) {
        log.info("[CANCEL_WORKFLOW] Successfully cancelled reservation targetId={}, newStatus={}",
                context.getTargetId(), context.getNewStatus());
    }

    @Override
    public void afterThrowProcess(WorkflowContext context, Throwable ex) {
        log.error("[CANCEL_WORKFLOW] Failed CANCEL reservation for targetId={}, error={}",
                context.getTargetId(), ex.getMessage());
    }
}
