package com.project.estate.workflow.strategy.reservation;

import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.ReservationActor;
import com.project.estate.workflow.context.WorkflowContext;
import com.project.estate.workflow.strategy.AbstractWorkflowStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PayDepositWorkflowStrategy extends AbstractWorkflowStrategy {

  @Override
  public ReservationAction getAction() {
    return ReservationAction.PAY_DEPOSIT;
  }

  @Override
  protected ReservationActor getDefaultActor() {
    return ReservationActor.CUSTOMER;
  }

  @Override
  protected void doAfterProcess(WorkflowContext context) {
    log.info(
        "[PAY_DEPOSIT_STRATEGY] Deposit paid successfully for targetId={}", context.getTargetId());
  }
}
