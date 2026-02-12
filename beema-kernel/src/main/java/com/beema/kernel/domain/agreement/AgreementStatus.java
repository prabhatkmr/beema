package com.beema.kernel.domain.agreement;

/**
 * Agreement lifecycle statuses.
 *
 * State transitions:
 * DRAFT → QUOTED → BOUND → ACTIVE → [CANCELLED | EXPIRED | RENEWED]
 */
public enum AgreementStatus {
    /**
     * Initial state - data entry in progress.
     */
    DRAFT,

    /**
     * Premium quoted, awaiting acceptance.
     */
    QUOTED,

    /**
     * Accepted and bound, awaiting effective date.
     */
    BOUND,

    /**
     * In force - coverage active.
     */
    ACTIVE,

    /**
     * Terminated before expiration.
     */
    CANCELLED,

    /**
     * Reached expiration date without renewal.
     */
    EXPIRED,

    /**
     * Replaced by renewal agreement.
     */
    RENEWED
}
