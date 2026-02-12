package com.beema.kernel.domain.metadata;

/**
 * Market contexts supported by the Beema platform.
 *
 * Each context has different business rules, workflows, and data schemas.
 */
public enum MarketContext {
    /**
     * Retail insurance - Personal lines (auto, home, etc.)
     * - Individual consumers
     * - Standardized products
     * - High volume, low premium
     */
    RETAIL,

    /**
     * Commercial insurance - Business lines (liability, property, etc.)
     * - Business entities
     * - Customized coverage
     * - Medium volume, medium premium
     */
    COMMERCIAL,

    /**
     * London Market - Specialty/reinsurance (marine, aviation, etc.)
     * - Complex risks
     * - Syndicated underwriting
     * - Low volume, high premium
     */
    LONDON_MARKET;

    /**
     * Parse from string with fallback.
     */
    public static MarketContext fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return MarketContext.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
