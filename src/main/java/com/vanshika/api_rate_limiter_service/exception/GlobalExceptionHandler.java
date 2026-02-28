package com.vanshika.api_rate_limiter_service.exception;

import com.vanshika.api_rate_limiter_service.model.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleGenericException(
      Exception ex) {

    return ResponseEntity.internalServerError()
        .body(new ApiResponse<>(
            false,
            "Internal Server Error",
            null));
  }
}
