package com.project.estate.workflow.strategy.reservation;

import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.ReservationActor;
import com.project.estate.workflow.context.WorkflowContext;
import com.project.estate.workflow.strategy.AbstractWorkflowStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CompleteReservationWorkflowStrategy extends AbstractWorkflowStrategy {

  @Override
  public ReservationAction getAction() {
    return ReservationAction.COMPLETE;
  }

  @Override
  protected ReservationActor getDefaultActor() {
    return ReservationActor.ADMIN;
  }

  @Override
  protected void doAfterProcess(WorkflowContext context) {
    log.info(
        "[COMPLETE_STRATEGY] Reservation completed successfully for targetId={}",
        context.getTargetId());
  }
}
