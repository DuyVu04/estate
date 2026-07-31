package com.project.estate.workflow;

import com.project.estate.entity.WorkflowHistory;
import com.project.estate.entity.WorkflowInstance;
import com.project.estate.enums.ReservationAction;
import com.project.estate.enums.WorkflowInstanceStatus;
import com.project.estate.repository.WorkflowHistoryRepository;
import com.project.estate.repository.WorkflowInstanceRepository;
import com.project.estate.workflow.annotation.WorkflowEngine;
import com.project.estate.workflow.aspect.WorkflowAspect;
import com.project.estate.workflow.factory.WorkflowStrategyFactory;
import com.project.estate.workflow.service.WorkflowPersistenceService;
import com.project.estate.workflow.strategy.WorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CancelReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CreateReservationWorkflowStrategy;
import com.project.estate.workflow.util.SpelEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowAspectTest {

    @Mock
    private WorkflowInstanceRepository instanceRepository;

    @Mock
    private WorkflowHistoryRepository historyRepository;

    private DummyService proxiedService;

    static class DummyService {
        @WorkflowEngine(
                action = ReservationAction.CREATE,
                step = "create-reservation",
                targetIdSpel = "#result"
        )
        public String createReservation(String customerId) {
            return "res-001";
        }

        @WorkflowEngine(
                action = ReservationAction.CANCEL,
                step = "cancel-reservation",
                targetIdSpel = "#reservationId"
        )
        public void cancelReservation(String reservationId) {
            // Cancel business logic
        }

        @WorkflowEngine(
                action = ReservationAction.CANCEL,
                step = "cancel-reservation",
                targetIdSpel = "#reservationId"
        )
        public void failingCancelReservation(String reservationId) {
            throw new RuntimeException("Simulated business error");
        }
    }

    @BeforeEach
    void setUp() {
        WorkflowPersistenceService persistenceService = new WorkflowPersistenceService(instanceRepository, historyRepository);
        SpelEvaluator spelEvaluator = new SpelEvaluator();

        CreateReservationWorkflowStrategy createStrategy = new CreateReservationWorkflowStrategy();
        ReflectionTestUtils.setField(createStrategy, "persistenceService", persistenceService);
        ReflectionTestUtils.setField(createStrategy, "spelEvaluator", spelEvaluator);

        CancelReservationWorkflowStrategy cancelStrategy = new CancelReservationWorkflowStrategy();
        ReflectionTestUtils.setField(cancelStrategy, "persistenceService", persistenceService);
        ReflectionTestUtils.setField(cancelStrategy, "spelEvaluator", spelEvaluator);

        List<WorkflowStrategy> strategies = Arrays.asList(createStrategy, cancelStrategy);
        WorkflowStrategyFactory strategyFactory = new WorkflowStrategyFactory(strategies);

        WorkflowAspect aspect = new WorkflowAspect(strategyFactory);

        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new DummyService());
        proxyFactory.addAspect(aspect);
        proxiedService = proxyFactory.getProxy();
    }

    @Test
    @DisplayName("Should intercept CREATE action method, save WorkflowInstance and WorkflowHistory")
    void interceptCreateReservation_Success() {
        when(instanceRepository.save(any(WorkflowInstance.class)))
                .thenAnswer(invocation -> {
                    WorkflowInstance inst = invocation.getArgument(0);
                    inst.setId("wf-inst-111");
                    return inst;
                });

        String reservationId = proxiedService.createReservation("cust-01");

        assertEquals("res-001", reservationId);
        verify(instanceRepository, times(1)).save(any(WorkflowInstance.class));
        verify(historyRepository, times(1)).save(any(WorkflowHistory.class));
    }

    @Test
    @DisplayName("Should intercept CANCEL action method, update WorkflowInstance and record history")
    void interceptCancelReservation_Success() {
        WorkflowInstance existingInstance = WorkflowInstance.builder()
                .id("wf-inst-222")
                .workflowName("reservation-workflow")
                .targetId("res-002")
                .status(WorkflowInstanceStatus.IN_PROGRESS)
                .currentStep("create-reservation")
                .build();

        when(instanceRepository.findByWorkflowNameAndTargetId("reservation-workflow", "res-002"))
                .thenReturn(Optional.of(existingInstance));

        when(instanceRepository.save(any(WorkflowInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> proxiedService.cancelReservation("res-002"));

        verify(instanceRepository, times(1)).save(any(WorkflowInstance.class));
        verify(historyRepository, times(1)).save(any(WorkflowHistory.class));
    }

    @Test
    @DisplayName("Should intercept Exception thrown by business method and record FAILED history in independent transaction")
    void interceptException_RecordsFailedWorkflowHistory() {
        WorkflowInstance existingInstance = WorkflowInstance.builder()
                .id("wf-inst-333")
                .workflowName("reservation-workflow")
                .targetId("res-003")
                .status(WorkflowInstanceStatus.IN_PROGRESS)
                .currentStep("create-reservation")
                .build();

        when(instanceRepository.findByWorkflowNameAndTargetId("reservation-workflow", "res-003"))
                .thenReturn(Optional.of(existingInstance));
        when(instanceRepository.save(any(WorkflowInstance.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> proxiedService.failingCancelReservation("res-003"));
        assertEquals("Simulated business error", ex.getMessage());

        verify(historyRepository, times(1)).save(argThat(hist ->
                hist.getStatus() == com.project.estate.enums.WorkflowHistoryStatus.FAILED &&
                "Simulated business error".equals(hist.getErrorMessage())
        ));
    }
}
