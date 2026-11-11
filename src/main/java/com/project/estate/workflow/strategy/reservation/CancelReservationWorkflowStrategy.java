package com.project.estate.workflow.strategy.reservation;

import com.project.estate.enums.ReservationAction;
import com.project.estate.workflow.context.WorkflowContext;
import com.project.estate.workflow.strategy.AbstractWorkflowStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CancelReservationWorkflowStrategy extends AbstractWorkflowStrategy {

  @Override
  public ReservationAction getAction() {
    return ReservationAction.CANCEL;
  }

  @Override
  protected void doAfterProcess(WorkflowContext context) {
    log.info(
        "[CANCEL_STRATEGY] Reservation cancelled successfully for targetId={}",
        context.getTargetId());
  }
}
