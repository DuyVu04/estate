package com.project.estate.modules.reservation.workflow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.project.estate.modules.identity.entity.User;
import com.project.estate.modules.identity.repository.UserRepository;
import com.project.estate.modules.property.entity.Property;
import com.project.estate.modules.property.enums.PropertyStatus;
import com.project.estate.modules.property.repository.PropertyRepository;
import com.project.estate.modules.reservation.dto.request.ReservationRequest;
import com.project.estate.modules.reservation.dto.response.ReservationResponse;
import com.project.estate.modules.reservation.entity.Reservation;
import com.project.estate.modules.reservation.enums.ReservationStatus;
import com.project.estate.modules.reservation.mapper.ReservationMapper;
import com.project.estate.modules.reservation.repository.ReservationRepository;
import com.project.estate.modules.reservation.service.ReservationService;
import com.project.estate.modules.reservation.service.ReservationTransactionalHandler;
import com.project.estate.modules.reservation.workflow.aspect.WorkflowAspect;
import com.project.estate.modules.reservation.workflow.entity.WorkflowInstance;
import com.project.estate.modules.reservation.workflow.factory.WorkflowStrategyFactory;
import com.project.estate.modules.reservation.workflow.repository.WorkflowHistoryRepository;
import com.project.estate.modules.reservation.workflow.repository.WorkflowInstanceRepository;
import com.project.estate.modules.reservation.workflow.service.WorkflowPersistenceService;
import com.project.estate.modules.reservation.workflow.strategy.WorkflowStrategy;
import com.project.estate.modules.reservation.workflow.strategy.reservation.CancelReservationWorkflowStrategy;
import com.project.estate.modules.reservation.workflow.strategy.reservation.CreateReservationWorkflowStrategy;
import com.project.estate.modules.reservation.workflow.util.SpelEvaluator;
import com.project.estate.shared.exception.AppException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReservationConcurrencyTest {

  @Mock private ReservationRepository reservationRepository;

  @Mock private PropertyRepository propertyRepository;

  @Mock private UserRepository userRepository;

  @Mock private ReservationMapper reservationMapper;

  @Mock private WorkflowInstanceRepository instanceRepository;

  @Mock private WorkflowHistoryRepository historyRepository;

  @Mock private org.redisson.api.RedissonClient redissonClient;

  @Mock private org.redisson.api.RLock rLock;

  @Mock private org.springframework.cache.CacheManager cacheManager;

  @Mock private org.springframework.cache.Cache cache;

  private ReservationService proxiedReservationService;

  @BeforeEach
  void setUp() throws InterruptedException {
    WorkflowPersistenceService persistenceService =
        new WorkflowPersistenceService(instanceRepository, historyRepository);
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

    ReservationTransactionalHandler targetHandler =
        new ReservationTransactionalHandler(
            reservationRepository, propertyRepository, userRepository, reservationMapper);

    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(targetHandler);
    proxyFactory.addAspect(aspect);
    ReservationTransactionalHandler proxiedHandler = proxyFactory.getProxy();

    lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
    lenient()
        .when(rLock.tryLock(anyLong(), any(java.util.concurrent.TimeUnit.class)))
        .thenReturn(true);
    lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);
    lenient().when(cacheManager.getCache(anyString())).thenReturn(cache);

    proxiedReservationService =
        new ReservationService(
            reservationRepository,
            reservationMapper,
            proxiedHandler,
            redissonClient,
            cacheManager,
            instanceRepository,
            historyRepository);
  }

  @Test
  @DisplayName(
      "Concurrency: Simultaneous reservation requests for the same property must allow exactly one success")
  void concurrentReservation_OnlyOneSucceeds() throws InterruptedException {
    String propertyId = "prop-concurrent-01";
    Property property =
        Property.builder()
            .id(propertyId)
            .status(PropertyStatus.AVAILABLE)
            .version(1L)
            .price(java.math.BigDecimal.valueOf(5000000000L))
            .build();

    User userA = User.builder().id("user-a").build();
    User userB = User.builder().id("user-b").build();

    lenient().when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
    lenient().when(userRepository.findById("user-a")).thenReturn(Optional.of(userA));
    lenient().when(userRepository.findById("user-b")).thenReturn(Optional.of(userB));

    // Atomic check simulation: first call returns false (not reserved), subsequent call returns
    // true (already reserved)
    AtomicInteger activeCheckCount = new AtomicInteger(0);
    when(reservationRepository.existsByPropertyIdAndStatus(propertyId, ReservationStatus.ACTIVE))
        .thenAnswer(inv -> activeCheckCount.getAndIncrement() > 0);

    when(instanceRepository.save(any(WorkflowInstance.class)))
        .thenAnswer(
            inv -> {
              WorkflowInstance inst = inv.getArgument(0);
              inst.setId("wf-inst-concurrent-1");
              return inst;
            });

    when(reservationRepository.save(any(Reservation.class)))
        .thenAnswer(
            inv -> {
              Reservation r = inv.getArgument(0);
              r.setId("res-concurrent-1");
              return r;
            });

    when(reservationMapper.toResponse(any(Reservation.class)))
        .thenReturn(
            ReservationResponse.builder()
                .id("res-concurrent-1")
                .status(ReservationStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build());

    int numberOfThreads = 2;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    executor.execute(
        () -> {
          try {
            latch.await();
            proxiedReservationService.reserve(new ReservationRequest(propertyId, "user-a"));
            successCount.incrementAndGet();
          } catch (AppException e) {
            failureCount.incrementAndGet();
          } catch (Exception ignored) {
          } finally {
            doneLatch.countDown();
          }
        });

    executor.execute(
        () -> {
          try {
            latch.await();
            proxiedReservationService.reserve(new ReservationRequest(propertyId, "user-b"));
            successCount.incrementAndGet();
          } catch (AppException e) {
            failureCount.incrementAndGet();
          } catch (Exception ignored) {
          } finally {
            doneLatch.countDown();
          }
        });

    // Fire both threads at the exact same moment
    latch.countDown();
    doneLatch.await();
    executor.shutdown();

    assertEquals(1, successCount.get(), "Exactly one reservation request should succeed");
    assertEquals(
        1,
        failureCount.get(),
        "Exactly one reservation request should be rejected due to concurrency control");
  }
}
