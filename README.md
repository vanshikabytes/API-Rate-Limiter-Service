# API Rate Limiter Service

A production-ready API Rate Limiting system built using Spring Boot.

## Tech Stack

- Java 17
- Spring Boot 4
- Maven
- YAML configuration

## Architecture

Layered architecture:

- Controller
- Service
- Repository
- Model

---

This repository will progressively implement:

- Phase 1: Core rate limiting
- Phase 2: Distributed rate limiting
- Phase 3: Advanced optimizations

## Current Implementation Status

### Phase 1 (In Progress)

- Token Bucket algorithm
- Thread-safe implementation
- In-memory bucket storage
- Service layer abstraction

## Design Decisions

### Token Bucket Algorithm

Implements O(1) rate limiting with time-based refill.

### Thread Safety

- AtomicLong used for token counter
- synchronized block ensures safe refill and consume operations
- ConcurrentHashMap ensures safe multi-user bucket storage

### Extensibility

Repository layer allows easy migration from in-memory storage to Redis or distributed cache in future phases.

## Flow

Client → Controller (Phase 4)
↓
RateLimiterService
↓
InMemoryBucketRepository
↓
TokenBucket
