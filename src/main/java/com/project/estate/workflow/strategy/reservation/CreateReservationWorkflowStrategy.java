package com.project.estate.workflow.strategy.reservation;

import com.project.estate.enums.ReservationAction;
import com.project.estate.workflow.context.WorkflowContext;
import com.project.estate.workflow.strategy.AbstractWorkflowStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CreateReservationWorkflowStrategy extends AbstractWorkflowStrategy {

    @Override
    public ReservationAction getAction() {
        return ReservationAction.CREATE;
    }

    @Override
    protected void doAfterProcess(WorkflowContext context) {
        log.info("[CREATE_STRATEGY] Reservation created successfully for targetId={}", context.getTargetId());
    }
}
