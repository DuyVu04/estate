package com.project.estate.exception;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.enums.ErrorCode;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
    ErrorCode errorCode = ex.getErrorCode();
    log.warn("[APP_EXCEPTION] code={}, message={}", errorCode.getCode(), errorCode.getMessage());

    return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.error(errorCode));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
      MethodArgumentNotValidException ex) {

    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    log.warn("[VALIDATION_ERROR] {}", errors);

    return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, errors));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {

    log.error("[UNEXPECTED_ERROR] {}", ex.getMessage(), ex);

    return ResponseEntity.internalServerError()
        .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
    ApiResponse<?> response = ApiResponse.error(ErrorCode.ACCESS_DENIED);
    log.error("[UNEXPECTED_ERROR] {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }

  @ExceptionHandler({
    org.springframework.orm.ObjectOptimisticLockingFailureException.class,
    jakarta.persistence.OptimisticLockException.class,
    org.springframework.dao.OptimisticLockingFailureException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(Exception ex) {
    log.warn("[CONCURRENCY_LOCK_FAILURE] Concurrent modification detected: {}", ex.getMessage());
    return ResponseEntity.status(ErrorCode.PROPERTY_ALREADY_RESERVED.getStatus())
        .body(ApiResponse.error(ErrorCode.PROPERTY_ALREADY_RESERVED));
  }
}
