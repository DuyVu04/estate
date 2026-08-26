package com.project.estate.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.project.estate.dto.request.ReservationRequest;
import com.project.estate.dto.response.ReservationResponse;
import com.project.estate.enums.ErrorCode;
import com.project.estate.exception.AppException;
import com.project.estate.mapper.ReservationMapper;
import com.project.estate.repository.ReservationRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class ReservationDistributedLockConcurrencyTest {

  @Mock private ReservationRepository reservationRepository;
  @Mock private ReservationMapper reservationMapper;
  @Mock private ReservationTransactionalHandler transactionalHandler;
  @Mock private RedissonClient redissonClient;
  @Mock private RLock rLock;
  @Mock private CacheManager cacheManager;
  @Mock private Cache cache;
  @Mock private com.project.estate.repository.WorkflowInstanceRepository workflowInstanceRepository;
  @Mock private com.project.estate.repository.WorkflowHistoryRepository workflowHistoryRepository;

  private ReservationService reservationService;

  @BeforeEach
  void setUp() {
    lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
    lenient().when(cacheManager.getCache(anyString())).thenReturn(cache);
    reservationService =
        new ReservationService(
            reservationRepository,
            reservationMapper,
            transactionalHandler,
            redissonClient,
            cacheManager,
            workflowInstanceRepository,
            workflowHistoryRepository);
  }

  @Test
  @DisplayName(
      "Multi-threaded Concurrency: 20 threads simultaneously reserving same property - only 1 succeeds")
  void testConcurrentReservations_ExactOneWinsLock() throws InterruptedException {
    int totalThreads = 20;
    ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
    CountDownLatch readyLatch = new CountDownLatch(totalThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(totalThreads);

    AtomicBoolean lockOccupied = new AtomicBoolean(false);
    AtomicInteger successfulReservations = new AtomicInteger(0);
    AtomicInteger rejectedReservations = new AtomicInteger(0);

    // Simulate Redisson RLock behavior: only the first thread gets the lock, others fail or wait
    when(rLock.tryLock(anyLong(), any(TimeUnit.class)))
        .thenAnswer(
            invocation -> {
              if (lockOccupied.compareAndSet(false, true)) {
                return true;
              }
              return false;
            });

    when(rLock.isHeldByCurrentThread()).thenReturn(true);

    ReservationRequest request = new ReservationRequest("prop-luxury-001", "user-001");
    ReservationResponse mockResponse =
        new ReservationResponse(
            "res-success-01",
            null,
            com.project.estate.enums.ReservationStatus.ACTIVE,
            java.time.LocalDateTime.now().plusMinutes(15));

    when(transactionalHandler.executeReserve(any(ReservationRequest.class)))
        .thenReturn(mockResponse);

    for (int i = 0; i < totalThreads; i++) {
      executor.submit(
          () -> {
            readyLatch.countDown();
            try {
              startLatch.await(); // Guarantee all 20 threads start at exact same millisecond
              reservationService.reserve(request);
              successfulReservations.incrementAndGet();
            } catch (AppException e) {
              if (e.getErrorCode() == ErrorCode.CONCURRENT_REQUEST) {
                rejectedReservations.incrementAndGet();
              }
            } catch (Exception e) {
              // other exceptions
            } finally {
              finishLatch.countDown();
            }
          });
    }

    readyLatch.await();
    startLatch.countDown(); // Fire!
    finishLatch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    // Verify exact 1 winner and 19 rejected safely
    assertEquals(1, successfulReservations.get(), "Exactly 1 thread must acquire lock and succeed");
    assertEquals(
        19,
        rejectedReservations.get(),
        "19 competing threads must be safely rejected with CONCURRENT_REQUEST");
    verify(transactionalHandler, times(1)).executeReserve(any(ReservationRequest.class));
    verify(cache, times(1)).evict("prop-luxury-001");
  }
}
