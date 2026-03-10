# API Rate Limiter Service

A production-ready API Rate Limiter Service built with Spring Boot, implementing the Token Bucket algorithm with multi-window support.

## 🚀 Key Features (Phase 2)
- **Multi-Window Support**: Separate limits for `SECOND`, `MINUTE`, and `HOUR` windows.
- **Per-Identifier Configuration**: Specific limits for `user`, `ip`, and `apiKey`.
- **Precise Headers**:
  - `X-RateLimit-Remaining`: Tokens currently available in the bucket.
  - `X-RateLimit-Capacity`: Maximum capacity of the bucket.
  - `X-RateLimit-Reset`: Unix epoch second when the bucket will be fully refilled.
  - `Retry-After`: (On 429) Seconds to wait for the next token.
- **Clean Architecture**: Decoupled Controller, Service, and Repository layers.
- **Configurable Cleanup**: Automatic removal of inactive buckets with configurable TTL and interval.

## 🛠️ API Endpoints
- `GET /api/rate-limit/{key}`: Check rate limit (defaults to Minute window).
- `GET /api/rate-limit/{window}/{key}`: Check rate limit for a specific window (`second`, `minute`, `hour`).
- `DELETE /api/rate-limit/{key}`: Reset rate limit for all windows associated with the key.
- `GET /api/rate-limit/status`: Check service health.

## ⚙️ Configuration
Example `application.yaml`:
```yaml
rate-limiter:
  cleanupTtlSeconds: 300
  cleanupIntervalMs: 60000
  limits:
    user:
      second: { capacity: 10, refillRate: 10 }
      minute: { capacity: 100, refillRate: 100 }
      hour:   { capacity: 1000, refillRate: 1000 }
```

## 🧪 Running Tests
```bash
./mvnw test
```
