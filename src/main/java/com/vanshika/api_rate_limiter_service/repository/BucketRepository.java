package com.vanshika.api_rate_limiter_service.repository;

import com.vanshika.api_rate_limiter_service.model.TokenBucket;

/**
 * Interface for token bucket storage.
 *
 * This abstraction allows us to switch from in-memory storage to a distributed
 * store like Redis without having to modify our service logic.
 *
 * Phase 2 extends this interface with a 3-phase reservation pattern:
 *   tryReserve  -> Atomically deduct a token and create a reservation record.
 *   commit      -> Finalize the reservation (no token change, just cleanup).
 *   rollback    -> Undo the reservation and refund the token.
 */
public interface BucketRepository {

    /**
     * Retrieves an existing bucket or initializes a new one for the given key.
     */
    TokenBucket getBucket(String key, long capacity, long refillTokens, long windowSeconds);

    /**
     * Clears a bucket entry, effectively resetting the rate limit for that key.
     */
    void removeBucket(String key);

    /**
     * Phase 1: Atomically reserves a token by deducting it and creating a
     * reservation record identified by reservationId.
     *
     * @return true if the token was successfully reserved, false if none available.
     */
    default boolean tryReserve(String key, long capacity, long refillTokens, long windowSeconds, String reservationId) {
        throw new UnsupportedOperationException("tryReserve not supported by this repository");
    }

    /**
     * Phase 3A: Commits a reservation. Deletes the reservation key.
     * The token was already deducted in Phase 1 so nothing else changes.
     */
    default void commit(String key, String reservationId) {
        throw new UnsupportedOperationException("commit not supported by this repository");
    }

    /**
     * Phase 3B: Rolls back a reservation. Deletes the reservation key and
     * refunds the token back to the bucket.
     */
    default void rollback(String key, String reservationId) {
        throw new UnsupportedOperationException("rollback not supported by this repository");
    }
}
