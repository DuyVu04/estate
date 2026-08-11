package com.project.estate.workflow;

import com.project.estate.enums.ReservationAction;
import com.project.estate.workflow.factory.WorkflowStrategyFactory;
import com.project.estate.workflow.strategy.WorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CancelReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CompleteReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CreateReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.ExpireReservationWorkflowStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowStrategyFactoryTest {

    private WorkflowStrategyFactory factory;

    @BeforeEach
    void setUp() {
        List<WorkflowStrategy> strategies = Arrays.asList(
                new CreateReservationWorkflowStrategy(),
                new CancelReservationWorkflowStrategy(),
                new CompleteReservationWorkflowStrategy(),
                new ExpireReservationWorkflowStrategy()
        );
        factory = new WorkflowStrategyFactory(strategies);
    }

    @Test
    @DisplayName("Should resolve CreateReservationWorkflowStrategy for CREATE action")
    void resolveCreateStrategy_Success() {
        WorkflowStrategy strategy = factory.getStrategy(ReservationAction.CREATE);
        assertNotNull(strategy);
        assertTrue(strategy instanceof CreateReservationWorkflowStrategy);
        assertEquals(ReservationAction.CREATE, strategy.getAction());
    }

    @Test
    @DisplayName("Should resolve CancelReservationWorkflowStrategy for CANCEL action")
    void resolveCancelStrategy_Success() {
        WorkflowStrategy strategy = factory.getStrategy(ReservationAction.CANCEL);
        assertNotNull(strategy);
        assertTrue(strategy instanceof CancelReservationWorkflowStrategy);
        assertEquals(ReservationAction.CANCEL, strategy.getAction());
    }

    @Test
    @DisplayName("Should resolve CompleteReservationWorkflowStrategy for COMPLETE action")
    void resolveCompleteStrategy_Success() {
        WorkflowStrategy strategy = factory.getStrategy(ReservationAction.COMPLETE);
        assertNotNull(strategy);
        assertTrue(strategy instanceof CompleteReservationWorkflowStrategy);
        assertEquals(ReservationAction.COMPLETE, strategy.getAction());
    }

    @Test
    @DisplayName("Should resolve ExpireReservationWorkflowStrategy for EXPIRE action")
    void resolveExpireStrategy_Success() {
        WorkflowStrategy strategy = factory.getStrategy(ReservationAction.EXPIRE);
        assertNotNull(strategy);
        assertTrue(strategy instanceof ExpireReservationWorkflowStrategy);
        assertEquals(ReservationAction.EXPIRE, strategy.getAction());
    }
}
