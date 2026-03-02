# API Rate Limiter Service

## 📌 Overview

A production-ready API Rate Limiting system built using Spring Boot.

This project implements the **Token Bucket Algorithm** to enforce API rate limits with configurable capacity and refill rate.

---

## Phase 1 – Completed

### Implemented Features

- Token Bucket rate limiting algorithm
- Thread-safe implementation
- Configurable capacity and refill rate
- HTTP 429 response when rate limit exceeded
- Rate limit headers:
  - `X-Rate-Limit-Remaining`
  - `X-Rate-Limit-Capacity`
  - `X-Rate-Limit-Refill-Rate`
- Manual reset endpoint
- Service status endpoint
- In-memory storage using `ConcurrentHashMap`
- Automatic cleanup of inactive buckets
- Unit tests (6 test cases)

---

## 🏗 Architecture

Layered architecture:

- **Controller**
- **Service**
- **Repository**
- **Model**
- **Exception Handling**
- **Configuration**

### Request Flow

Client  
↓  
Controller  
↓  
RateLimiterService  
↓  
InMemoryBucketRepository  
↓  
TokenBucket

---

## ⚙ Configuration

Configured via `application.yaml`

```yaml
rate-limiter:
  capacity: 10
  refillRate: 5
```
