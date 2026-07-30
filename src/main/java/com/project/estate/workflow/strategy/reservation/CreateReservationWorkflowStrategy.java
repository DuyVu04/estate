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
public class CreateReservationWorkflowStrategy implements WorkflowStrategy {

    @Override
    public ReservationAction getAction() {
        return ReservationAction.CREATE;
    }

    @Override
    public void beforeProcess(WorkflowContext context) {
        log.info("[CREATE_WORKFLOW] Validating CREATE reservation transition for targetId={}", context.getTargetId());
        // Validate state transition using State Machine
        ReservationStatus nextStatus = ReservationStateMachine.getNextState(null, getAction(), context.getActor());
        context.setPreviousStatus(null);
        context.setNewStatus(nextStatus);
    }

    @Override
    public void afterProcess(WorkflowContext context) {
        log.info("[CREATE_WORKFLOW] Successfully processed CREATE reservation for targetId={}, status={}",
                context.getTargetId(), context.getNewStatus());
    }

    @Override
    public void afterThrowProcess(WorkflowContext context, Throwable ex) {
        log.error("[CREATE_WORKFLOW] Failed CREATE reservation for targetId={}, error={}",
                context.getTargetId(), ex.getMessage());
    }
}
