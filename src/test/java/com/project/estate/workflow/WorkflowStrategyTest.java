package com.project.estate.workflow;

import static org.junit.jupiter.api.Assertions.*;

import com.project.estate.enums.ReservationAction;
import com.project.estate.workflow.strategy.WorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CancelReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CompleteReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CreateReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.ExpireReservationWorkflowStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkflowStrategyTest {

  @Test
  @DisplayName("CreateReservationWorkflowStrategy action mapping test")
  void createReservationStrategy_Action() {
    WorkflowStrategy strategy = new CreateReservationWorkflowStrategy();
    assertEquals(ReservationAction.CREATE, strategy.getAction());
  }

  @Test
  @DisplayName("CancelReservationWorkflowStrategy action mapping test")
  void cancelReservationStrategy_Action() {
    WorkflowStrategy strategy = new CancelReservationWorkflowStrategy();
    assertEquals(ReservationAction.CANCEL, strategy.getAction());
  }

  @Test
  @DisplayName("CompleteReservationWorkflowStrategy action mapping test")
  void completeReservationStrategy_Action() {
    WorkflowStrategy strategy = new CompleteReservationWorkflowStrategy();
    assertEquals(ReservationAction.COMPLETE, strategy.getAction());
  }

  @Test
  @DisplayName("ExpireReservationWorkflowStrategy action mapping test")
  void expireReservationStrategy_Action() {
    WorkflowStrategy strategy = new ExpireReservationWorkflowStrategy();
    assertEquals(ReservationAction.EXPIRE, strategy.getAction());
  }
}
