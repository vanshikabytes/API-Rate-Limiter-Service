package com.vanshika.api_rate_limiter_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for updating a user's tier.
 * 
 * Enforces strict validation on tier names to match supported system tiers.
 */
@Getter
@Setter
public class UpdateTierRequest {

    /**
     * The new tier to assign to the user.
     * Must be one of the three supported levels: free, gold, or premium.
     */
    @NotBlank(message = "tier must not be blank")
    @Pattern(regexp = "free|gold|premium", message = "tier must be one of: free, gold, premium")
    private String tier;
}
