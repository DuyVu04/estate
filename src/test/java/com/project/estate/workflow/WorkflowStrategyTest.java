package com.project.estate.workflow;

import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.ReservationActor;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.workflow.context.WorkflowContext;
import com.project.estate.workflow.strategy.WorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CancelReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CompleteReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CreateReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.ExpireReservationWorkflowStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowStrategyTest {

    @Test
    @DisplayName("CreateReservationWorkflowStrategy lifecycle test")
    void createReservationStrategy_Lifecycle() {
        WorkflowStrategy strategy = new CreateReservationWorkflowStrategy();
        assertEquals(ReservationAction.CREATE, strategy.getAction());

        WorkflowContext context = WorkflowContext.builder()
                .targetId("res-101")
                .actor(ReservationActor.CUSTOMER)
                .action(ReservationAction.CREATE)
                .build();

        assertDoesNotThrow(() -> strategy.beforeProcess(context));
        assertEquals(ReservationStatus.ACTIVE, context.getNewStatus());
        assertDoesNotThrow(() -> strategy.afterProcess(context));
        assertDoesNotThrow(() -> strategy.afterThrowProcess(context, new RuntimeException("DB Error")));
    }

    @Test
    @DisplayName("CancelReservationWorkflowStrategy lifecycle test")
    void cancelReservationStrategy_Lifecycle() {
        WorkflowStrategy strategy = new CancelReservationWorkflowStrategy();
        assertEquals(ReservationAction.CANCEL, strategy.getAction());

        WorkflowContext context = WorkflowContext.builder()
                .targetId("res-102")
                .previousStatus(ReservationStatus.ACTIVE)
                .actor(ReservationActor.CUSTOMER)
                .action(ReservationAction.CANCEL)
                .build();

        assertDoesNotThrow(() -> strategy.beforeProcess(context));
        assertEquals(ReservationStatus.CANCELLED, context.getNewStatus());
        assertDoesNotThrow(() -> strategy.afterProcess(context));
    }

    @Test
    @DisplayName("CompleteReservationWorkflowStrategy lifecycle test")
    void completeReservationStrategy_Lifecycle() {
        WorkflowStrategy strategy = new CompleteReservationWorkflowStrategy();
        assertEquals(ReservationAction.COMPLETE, strategy.getAction());

        WorkflowContext context = WorkflowContext.builder()
                .targetId("res-103")
                .previousStatus(ReservationStatus.ACTIVE)
                .actor(ReservationActor.ADMIN)
                .action(ReservationAction.COMPLETE)
                .build();

        assertDoesNotThrow(() -> strategy.beforeProcess(context));
        assertEquals(ReservationStatus.COMPLETED, context.getNewStatus());
        assertDoesNotThrow(() -> strategy.afterProcess(context));
    }

    @Test
    @DisplayName("ExpireReservationWorkflowStrategy lifecycle test")
    void expireReservationStrategy_Lifecycle() {
        WorkflowStrategy strategy = new ExpireReservationWorkflowStrategy();
        assertEquals(ReservationAction.EXPIRE, strategy.getAction());

        WorkflowContext context = WorkflowContext.builder()
                .targetId("res-104")
                .previousStatus(ReservationStatus.ACTIVE)
                .actor(ReservationActor.SYSTEM)
                .action(ReservationAction.EXPIRE)
                .build();

        assertDoesNotThrow(() -> strategy.beforeProcess(context));
        assertEquals(ReservationStatus.EXPIRED, context.getNewStatus());
        assertDoesNotThrow(() -> strategy.afterProcess(context));
    }
}
