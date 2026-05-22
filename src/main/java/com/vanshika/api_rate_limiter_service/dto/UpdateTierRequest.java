package com.vanshika.api_rate_limiter_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request DTO for updating a user's rate-limit tier.
 *
 * Accepts the four Phase-2 tier names (case-insensitive) plus an optional
 * expiration timestamp.  When expiresAt is provided the system will automatically
 * downgrade the user to FREE once that instant has passed.
 */
@Getter
@Setter
public class UpdateTierRequest {

    /**
     * The new tier to assign to the user.
     * Must be one of the four supported levels: FREE, PRO, ENTERPRISE, UNLIMITED.
     * Validation is case-insensitive via the regex flag (?i).
     */
    @NotBlank(message = "tier must not be blank")
    @Pattern(
        regexp = "(?i)FREE|PRO|ENTERPRISE|UNLIMITED",
        message = "tier must be one of: FREE, PRO, ENTERPRISE, UNLIMITED"
    )
    private String tier;

    /**
     * Optional ISO-8601 timestamp at which the tier should automatically expire.
     * When the tier expires the user is treated as FREE until explicitly upgraded.
     * Set to null for a non-expiring tier assignment.
     */
    private LocalDateTime expiresAt;
}
