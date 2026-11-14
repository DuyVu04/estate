package com.project.estate.exception;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.enums.ErrorCode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
  private final MessageSource messageSource;

  private String resolveMessage(ErrorCode errorCode) {
    if (messageSource == null) {
      return errorCode.getMessage();
    }
    Locale locale = LocaleContextHolder.getLocale();
    try {
      return messageSource.getMessage(errorCode.name(), null, errorCode.getMessage(), locale);
    } catch (Exception e) {
      return errorCode.getMessage();
    }
  }

  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
    ErrorCode errorCode = ex.getErrorCode();
    String message = resolveMessage(errorCode);
    log.warn("[APP_EXCEPTION] code={}, message={}", errorCode.getCode(), message);

    return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.error(errorCode, message));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
      MethodArgumentNotValidException ex) {

    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    log.warn("[VALIDATION_ERROR] {}", errors);
    String message = resolveMessage(ErrorCode.VALIDATION_ERROR);

    return ResponseEntity.badRequest()
        .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, message, errors));
  }

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  public ResponseEntity<ApiResponse<Void>> handleBadRequestException(RuntimeException ex) {
    log.warn("[BAD_REQUEST] Client sent invalid syntax or arguments: {}", ex.getMessage());
    String message = resolveMessage(ErrorCode.INVALID_REQUEST);
    return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.INVALID_REQUEST, message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
    log.error("[UNEXPECTED_ERROR] {}", ex.getMessage(), ex);
    String message = resolveMessage(ErrorCode.INTERNAL_SERVER_ERROR);
    return ResponseEntity.internalServerError()
        .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, message));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
    log.error("[ACCESS_DENIED] {}", ex.getMessage(), ex);
    String message = resolveMessage(ErrorCode.ACCESS_DENIED);
    ApiResponse<?> response = ApiResponse.error(ErrorCode.ACCESS_DENIED, message);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }

  @ExceptionHandler({
    org.springframework.orm.ObjectOptimisticLockingFailureException.class,
    jakarta.persistence.OptimisticLockException.class,
    org.springframework.dao.OptimisticLockingFailureException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(Exception ex) {
    log.warn("[CONCURRENCY_LOCK_FAILURE] Concurrent modification detected: {}", ex.getMessage());
    String message = resolveMessage(ErrorCode.PROPERTY_ALREADY_RESERVED);
    return ResponseEntity.status(ErrorCode.PROPERTY_ALREADY_RESERVED.getStatus())
        .body(ApiResponse.error(ErrorCode.PROPERTY_ALREADY_RESERVED, message));
  }
}
