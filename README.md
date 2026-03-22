# API Rate Limiter Service (Phase 2)

A distributed, production-ready API Rate Limiter Service built with Spring Boot, Redis, and Lua scripting.

## 🚀 Key Features

- **Distributed Rate Limiting**: Uses Redis as a centralized token store, allowing multiple service instances to share the same rate limits.
- **Advanced Rules Engine**: Path-based matching using Ant-style patterns (e.g., `/api/search/**`). Supports method-specific limits and rule priorities.
- **Atomic Operations**: Core logic implemented in Lua scripts to ensure absolute thread-safety and zero race conditions in Redis.
- **High-Availability Fallback**: Automatically falls back to a local In-Memory rate limiter if the Redis server goes offline, ensuring 100% uptime.
- **Professional Metadata**: Returns standard HTTP headers (`X-RateLimit-Remaining`, `Retry-After`) and detailed JSON responses.

---

## 🛠️ Prerequisites

- **Java 17** or higher
- **Maven 3.x**
- **Redis Server** or **Docker** (For local development)

---

## 🏃 Getting Started

### 1. Start Redis
If you have Docker:
```bash
docker run -p 6379:6379 redis
```
Or run `redis-server.exe` if installed locally.

### 2. Configure Rules
Edit `src/main/resources/application.yaml` to define your custom rate limiting rules:
```yaml
rate-limiter:
  rules:
    - name: "search-api"
      path: "/api/search/**"
      method: "GET"
      capacity: 10
      refillRate: 10
      window: "MINUTE"
      priority: 100
```

### 3. Run the Application
```bash
./mvnw spring-boot:run
```

---

## 📡 API Endpoints

### 🟢 Global Limit Check
`GET /api/rate-limit/{identifier}`
- Tracks simple limits (Defaults: 5/min).
- Headers: `X-RateLimit-Remaining`, `X-RateLimit-Capacity`.

### 🔵 Advanced Rule Check
`GET /api/rate-limit/check?path=/api/search/items&method=GET&identifier=user_123`
- Matches against the Rules Engine.
- Returns detailed wait times if blocked.

### 🔴 Reset Limit
`DELETE /api/rate-limit/{identifier}`
- Clears all buckets associated with the identifier in both Redis and Memory.

---

## 📂 Architecture (Day-wise)

- **Day 1**: Redis & Infrastructure setup.
- **Day 2**: Distributed Token Bucket logic (Lua + Redis).
- **Day 3**: Advanced Rules Engine foundational matching.
- **Day 4**: Reliability & Fallback logic (Graceful Degradation).
- **Day 5**: Response Polishing & Professional Documentation.

---

## 👨‍💻 Author
**Vanshika**
Developed as part of the Technical Assessment.
