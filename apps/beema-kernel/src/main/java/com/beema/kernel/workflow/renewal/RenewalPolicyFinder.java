package com.beema.kernel.workflow.renewal;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface for finding policies that need renewal.
 */
public interface RenewalPolicyFinder {

    /**
     * Find policies expiring on a specific date.
     *
     * @param expiryDate Target expiry date
     * @return List of policies expiring on that date
     */
    List<RenewalPolicyInfo> findPoliciesExpiringOn(LocalDateTime expiryDate);
}
