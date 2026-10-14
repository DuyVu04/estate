package com.project.estate.shared.exception;

import com.project.estate.shared.enums.ErrorCode;
import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
  private final ErrorCode errorCode;

  public AppException(ErrorCode errorCode) {
    super(errorCode.getMessage());

    this.errorCode = errorCode;
  }
}
