package com.beema.kernel.ai.service;

import com.beema.kernel.ai.util.OpenRouterModels;
import com.beema.kernel.domain.claim.Claim;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Intelligently selects the best model based on claim characteristics
 *
 * This service provides automatic model selection based on claim attributes
 * such as value, complexity, and type. This allows for cost optimization
 * while maintaining accuracy where it matters most.
 */
@Service
public class ModelSelectionService {

    @Value("${beema.ai.model:openai/gpt-4-turbo-preview}")
    private String defaultModel;

    @Value("${beema.ai.auto-select-model:false}")
    private boolean autoSelectModel;

    /**
     * Selects the most appropriate model for analyzing a given claim
     *
     * @param claim The claim to analyze
     * @return The OpenRouter model identifier to use
     */
    public String selectModelForClaim(Claim claim) {
        if (!autoSelectModel) {
            return defaultModel;
        }

        // High-value claims: Use most accurate model
        if (claim.getClaimAmount() != null && claim.getClaimAmount() > 100000) {
            return OpenRouterModels.RECOMMENDED_ACCURACY;
        }

        // Complex claim types: Use balanced model
        if (isComplexClaimType(claim.getClaimType())) {
            return OpenRouterModels.RECOMMENDED_BALANCED;
        }

        // Standard claims: Use faster, cheaper model
        return OpenRouterModels.RECOMMENDED_SPEED;
    }

    /**
     * Determines if a claim type is considered complex
     *
     * @param claimType The type of claim
     * @return true if the claim type is complex
     */
    private boolean isComplexClaimType(String claimType) {
        if (claimType == null) {
            return false;
        }

        return claimType.contains("liability")
            || claimType.contains("professional_indemnity")
            || claimType.contains("complex")
            || claimType.contains("reinsurance");
    }
}
