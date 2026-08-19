package com.project.estate.service;

import com.project.estate.dto.request.ReservationRequest;
import com.project.estate.dto.response.ReservationResponse;
import com.project.estate.entity.Reservation;
import com.project.estate.enums.ErrorCode;
import com.project.estate.exception.AppException;
import com.project.estate.mapper.ReservationMapper;
import com.project.estate.repository.ReservationRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

  private final ReservationRepository reservationRepository;
  private final ReservationMapper reservationMapper;
  private final ReservationTransactionalHandler transactionalHandler;
  private final RedissonClient redissonClient;
  private final CacheManager cacheManager;

  @Value("${app.lock.reservation-wait-time-seconds:3}")
  private long waitTimeSeconds;

  public ReservationResponse reserve(ReservationRequest reservationRequest) {
    String lockKey = "lock:property:" + reservationRequest.propertyId();
    RLock lock = redissonClient.getLock(lockKey);

    boolean isAcquired = false;
    try {
      // 1. Chờ lấy lock tối đa waitTimeSeconds (Watchdog tự động kích hoạt)
      isAcquired = lock.tryLock(waitTimeSeconds, TimeUnit.SECONDS);
      if (!isAcquired) {
        log.warn("Tranh chấp lock thất bại cho propertyId: {}", reservationRequest.propertyId());
        throw new AppException(ErrorCode.CONCURRENT_REQUEST);
      }

      // 2. Thực thi Transaction DB (Commit thành công trước khi method trả về)
      ReservationResponse response = transactionalHandler.executeReserve(reservationRequest);

      // 3. Xóa cache TRƯỚC KHI nhả lock (triệt tiêu hoàn toàn stale read)
      evictPropertyCache(reservationRequest.propertyId());

      return response;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Thread bị ngắt khi đang đợi lock: {}", lockKey, e);
      throw new AppException(ErrorCode.CONCURRENT_REQUEST);
    } finally {
      // 4. Mở lock an toàn nếu thread hiện tại đang giữ lock
      if (isAcquired && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public Page<ReservationResponse> getReservations(
      Specification<Reservation> specification, Pageable pageable) {
    return reservationRepository
        .findAll(specification, pageable)
        .map(reservationMapper::toResponse);
  }

  public void cancelReservation(String id) {
    Reservation reservation =
        reservationRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

    String propertyId = reservation.getProperty().getId();
    String lockKey = "lock:property:" + propertyId;
    RLock lock = redissonClient.getLock(lockKey);

    boolean isAcquired = false;
    try {
      isAcquired = lock.tryLock(waitTimeSeconds, TimeUnit.SECONDS);
      if (!isAcquired) {
        throw new AppException(ErrorCode.CONCURRENT_REQUEST);
      }

      transactionalHandler.executeCancel(id);
      evictPropertyCache(propertyId);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AppException(ErrorCode.CONCURRENT_REQUEST);
    } finally {
      if (isAcquired && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  public void completeReservation(String id) {
    transactionalHandler.executeComplete(id);
  }

  public void expireReservation(String id) {
    transactionalHandler.executeExpire(id);
  }

  public void payDeposit(String id) {
    transactionalHandler.executePayDeposit(id);
  }

  private void evictPropertyCache(String propertyId) {
    try {
      var cache = cacheManager.getCache("property_detail");
      if (cache != null) {
        cache.evict(propertyId);
        log.info("Đã xóa cache property_detail cho ID: {}", propertyId);
      }
    } catch (Exception e) {
      log.error("Lỗi khi xóa cache property_detail cho ID: {}", propertyId, e);
    }
  }
}
