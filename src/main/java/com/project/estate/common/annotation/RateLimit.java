package com.project.estate.common.annotation;

import com.project.estate.enums.ErrorCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String key();

    int limit();

    long duration();

    TimeUnit unit();

    ErrorCode errorCode() default ErrorCode.TOO_MANY_REQUESTS;
}