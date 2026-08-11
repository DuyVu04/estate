package com.project.estate.workflow;

import com.project.estate.dto.request.ReservationRequest;
import com.project.estate.dto.response.ReservationResponse;
import com.project.estate.entity.Property;
import com.project.estate.entity.Reservation;
import com.project.estate.entity.User;
import com.project.estate.entity.WorkflowHistory;
import com.project.estate.entity.WorkflowInstance;
import com.project.estate.enums.PropertyStatus;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.enums.WorkflowInstanceStatus;
import com.project.estate.mapper.ReservationMapper;
import com.project.estate.repository.PropertyRepository;
import com.project.estate.repository.ReservationRepository;
import com.project.estate.repository.UserRepository;
import com.project.estate.repository.WorkflowHistoryRepository;
import com.project.estate.repository.WorkflowInstanceRepository;
import com.project.estate.service.ReservationService;
import com.project.estate.workflow.aspect.WorkflowAspect;
import com.project.estate.workflow.factory.WorkflowStrategyFactory;
import com.project.estate.workflow.service.WorkflowPersistenceService;
import com.project.estate.workflow.strategy.WorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CancelReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CompleteReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CreateReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.ExpireReservationWorkflowStrategy;
import com.project.estate.workflow.util.SpelEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceWorkflowTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private WorkflowInstanceRepository instanceRepository;

    @Mock
    private WorkflowHistoryRepository historyRepository;

    private ReservationService proxiedReservationService;

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

        CompleteReservationWorkflowStrategy completeStrategy = new CompleteReservationWorkflowStrategy();
        ReflectionTestUtils.setField(completeStrategy, "persistenceService", persistenceService);
        ReflectionTestUtils.setField(completeStrategy, "spelEvaluator", spelEvaluator);

        ExpireReservationWorkflowStrategy expireStrategy = new ExpireReservationWorkflowStrategy();
        ReflectionTestUtils.setField(expireStrategy, "persistenceService", persistenceService);
        ReflectionTestUtils.setField(expireStrategy, "spelEvaluator", spelEvaluator);

        List<WorkflowStrategy> strategies = Arrays.asList(createStrategy, cancelStrategy, completeStrategy, expireStrategy);
        WorkflowStrategyFactory strategyFactory = new WorkflowStrategyFactory(strategies);

        WorkflowAspect aspect = new WorkflowAspect(strategyFactory);

        ReservationService targetService = new ReservationService(
                reservationRepository, propertyRepository, userRepository, reservationMapper
        );

        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(targetService);
        proxyFactory.addAspect(aspect);
        proxiedReservationService = proxyFactory.getProxy();
    }

    @Test
    @DisplayName("End-to-End: Reserve property triggering CREATE workflow action")
    void reserveProperty_TriggersWorkflowEngine() {
        Property property = Property.builder().id("prop-01").status(PropertyStatus.AVAILABLE).price(java.math.BigDecimal.valueOf(5000000000L)).build();
        User user = User.builder().id("user-01").build();
        ReservationRequest request = new ReservationRequest("prop-01", "user-01");

        when(propertyRepository.findById("prop-01")).thenReturn(Optional.of(property));
        when(reservationRepository.existsByPropertyIdAndStatus("prop-01", ReservationStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findById("user-01")).thenReturn(Optional.of(user));

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId("res-999");
            return r;
        });

        when(reservationMapper.toResponse(any(Reservation.class)))
                .thenReturn(new ReservationResponse("res-999", null, ReservationStatus.ACTIVE, LocalDateTime.now().plusMinutes(15)));

        when(instanceRepository.save(any(WorkflowInstance.class)))
                .thenAnswer(inv -> {
                    WorkflowInstance inst = inv.getArgument(0);
                    inst.setId("wf-inst-999");
                    return inst;
                });

        ReservationResponse response = proxiedReservationService.reserve(request);

        assertNotNull(response);
        assertEquals("res-999", response.id());
        assertEquals(PropertyStatus.RESERVED, property.getStatus());

        verify(instanceRepository, times(1)).save(any(WorkflowInstance.class));
        verify(historyRepository, times(1)).save(any(WorkflowHistory.class));
    }

    @Test
    @DisplayName("End-to-End: Cancel reservation triggering CANCEL workflow action")
    void cancelReservation_TriggersWorkflowEngine() {
        Property property = Property.builder().id("prop-01").status(PropertyStatus.RESERVED).price(java.math.BigDecimal.valueOf(5000000000L)).build();
        Reservation reservation = Reservation.builder()
                .id("res-999")
                .property(property)
                .status(ReservationStatus.ACTIVE)
                .build();

        WorkflowInstance existingInstance = WorkflowInstance.builder()
                .id("wf-inst-999")
                .workflowName("reservation-workflow")
                .targetId("res-999")
                .status(WorkflowInstanceStatus.IN_PROGRESS)
                .currentStep("create-reservation")
                .build();

        when(reservationRepository.findById("res-999")).thenReturn(Optional.of(reservation));
        when(instanceRepository.findByWorkflowNameAndTargetId("reservation-workflow", "res-999"))
                .thenReturn(Optional.of(existingInstance));
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        proxiedReservationService.cancelReservation("res-999");

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(PropertyStatus.AVAILABLE, property.getStatus());

        verify(instanceRepository, times(1)).save(any(WorkflowInstance.class));
        verify(historyRepository, times(1)).save(any(WorkflowHistory.class));
    }

    @Test
    @DisplayName("End-to-End: Complete reservation triggering COMPLETE workflow action")
    void completeReservation_TriggersWorkflowEngine() {
        Property property = Property.builder().id("prop-01").status(PropertyStatus.RESERVED).price(java.math.BigDecimal.valueOf(5000000000L)).build();
        Reservation reservation = Reservation.builder()
                .id("res-999")
                .property(property)
                .status(ReservationStatus.ACTIVE)
                .build();

        WorkflowInstance existingInstance = WorkflowInstance.builder()
                .id("wf-inst-999")
                .workflowName("reservation-workflow")
                .targetId("res-999")
                .status(WorkflowInstanceStatus.IN_PROGRESS)
                .currentStep("create-reservation")
                .build();

        when(reservationRepository.findById("res-999")).thenReturn(Optional.of(reservation));
        when(instanceRepository.findByWorkflowNameAndTargetId("reservation-workflow", "res-999"))
                .thenReturn(Optional.of(existingInstance));
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        proxiedReservationService.completeReservation("res-999");

        assertEquals(ReservationStatus.COMPLETED, reservation.getStatus());
        assertEquals(PropertyStatus.SOLD, property.getStatus());

        verify(instanceRepository, times(1)).save(any(WorkflowInstance.class));
        verify(historyRepository, times(1)).save(any(WorkflowHistory.class));
    }
}
