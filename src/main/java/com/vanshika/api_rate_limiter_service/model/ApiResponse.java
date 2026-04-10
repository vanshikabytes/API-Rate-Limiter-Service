package com.vanshika.api_rate_limiter_service.model;

/**
 * Standard API envelope for all responses.
 * 
 * Using a consistent structure helps frontend developers and API clients
 * handle successes and errors uniformly.
 */
public class ApiResponse<T> {

  private boolean success;
  private String message;
  private T data;

  public ApiResponse(boolean success, String message, T data) {
    this.success = success;
    this.message = message;
    this.data = data;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public T getData() {
    return data;
  }
}