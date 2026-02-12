package com.beema.kernel.domain.agreement;

/**
 * Party roles in an insurance agreement.
 */
public enum PartyRole {
    /** The insured party (policyholder) */
    INSURED,

    /** Additional insured party */
    ADDITIONAL_INSURED,

    /** Named insured party */
    NAMED_INSURED,

    /** Insurance company providing coverage */
    INSURER,

    /** Insurance broker/agent */
    BROKER,

    /** Reinsurer (London Market) */
    REINSURER,

    /** Ceding company (London Market) */
    CEDING_COMPANY,

    /** Loss payee (e.g., mortgage lender) */
    LOSS_PAYEE,

    /** Beneficiary */
    BENEFICIARY
}
