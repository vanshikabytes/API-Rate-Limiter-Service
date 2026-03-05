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
- Unit tests
- Detailed Postman collection

---

## 🏗 Architecture

Layered architecture:
- **Controller**: REST endpoints and header management
- **Service**: Business logic and config resolution
- **Repository**: In-memory bucket storage
- **Model**: Token Bucket implementation

---

## 🚀 How to Run

### Prerequisites
- JDK 17 or higher
- Maven (or use provided `./mvnw`)

### Steps
1. **Clone the repository** (if not already done).
2. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```
3. **Test APIs**:
   Import `API-Rate-Limiter-Collection.json` into Postman and try the requests.

---

## ⚙ Configuration

Configured via `src/main/resources/application.yaml`. You can define limits per-identifier type (`user`, `ip`, `apiKey`).

```yaml
rate-limiter:
  limits:
    user:
      capacity: 100
      refillRate: 100
      windowSeconds: 60
```
