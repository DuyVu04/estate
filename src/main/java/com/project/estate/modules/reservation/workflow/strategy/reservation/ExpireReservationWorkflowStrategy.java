package com.project.estate.modules.reservation.workflow.strategy.reservation;

import com.project.estate.modules.reservation.enums.ReservationAction;
import com.project.estate.modules.reservation.enums.ReservationActor;
import com.project.estate.modules.reservation.workflow.context.WorkflowContext;
import com.project.estate.modules.reservation.workflow.strategy.AbstractWorkflowStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExpireReservationWorkflowStrategy extends AbstractWorkflowStrategy {

  @Override
  public ReservationAction getAction() {
    return ReservationAction.EXPIRE;
  }

  @Override
  protected ReservationActor getDefaultActor() {
    return ReservationActor.SYSTEM;
  }

  @Override
  protected void doAfterProcess(WorkflowContext context) {
    log.info(
        "[EXPIRE_STRATEGY] Reservation expired successfully for targetId={}",
        context.getTargetId());
  }
}
