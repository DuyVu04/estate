package com.project.estate.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.estate.enums.ErrorCode;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T result) {
  public static <T> ApiResponse<T> success(T result) {
    return ApiResponse.<T>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .result(result)
        .build();
  }

  public static ApiResponse<Void> success() {
    return ApiResponse.<Void>builder()
        .code(ErrorCode.SUCCESS.getCode())
        .message(ErrorCode.SUCCESS.getMessage())
        .build();
  }

  public static ApiResponse<Void> error(ErrorCode errorCode) {
    return ApiResponse.<Void>builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .build();
  }

  public static <T> ApiResponse<T> error(ErrorCode errorCode, T result) {
    return ApiResponse.<T>builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .result(result)
        .build();
  }
}
