package com.project.estate.workflow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.project.estate.dto.request.ReservationRequest;
import com.project.estate.dto.response.ReservationResponse;
import com.project.estate.entity.Property;
import com.project.estate.entity.Reservation;
import com.project.estate.entity.User;
import com.project.estate.entity.WorkflowInstance;
import com.project.estate.enums.PropertyStatus;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.exception.AppException;
import com.project.estate.mapper.ReservationMapper;
import com.project.estate.repository.PropertyRepository;
import com.project.estate.repository.ReservationRepository;
import com.project.estate.repository.UserRepository;
import com.project.estate.repository.WorkflowHistoryRepository;
import com.project.estate.repository.WorkflowInstanceRepository;
import com.project.estate.service.ReservationService;
import com.project.estate.service.ReservationTransactionalHandler;
import com.project.estate.workflow.aspect.WorkflowAspect;
import com.project.estate.workflow.factory.WorkflowStrategyFactory;
import com.project.estate.workflow.service.WorkflowPersistenceService;
import com.project.estate.workflow.strategy.WorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CancelReservationWorkflowStrategy;
import com.project.estate.workflow.strategy.reservation.CreateReservationWorkflowStrategy;
import com.project.estate.workflow.util.SpelEvaluator;
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
            reservationRepository, reservationMapper, proxiedHandler, redissonClient, cacheManager);
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
            new ReservationResponse(
                "res-concurrent-1",
                null,
                ReservationStatus.ACTIVE,
                LocalDateTime.now().plusMinutes(15)));

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
