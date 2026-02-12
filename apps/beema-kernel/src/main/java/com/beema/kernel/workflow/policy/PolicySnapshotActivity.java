package com.beema.kernel.workflow.policy;

import com.beema.kernel.workflow.policy.model.PolicySnapshot;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Policy Snapshot Activity Interface
 *
 * Defines activities for retrieving, storing, and notifying about policy snapshots.
 * Activities are retryable operations that interact with external systems.
 */
@ActivityInterface
public interface PolicySnapshotActivity {

    /**
     * Retrieves a snapshot of the policy data from the policy system.
     * This operation is retryable in case of network failures.
     *
     * @param policyId The policy ID to retrieve
     * @return PolicySnapshot containing the policy data
     */
    @ActivityMethod
    PolicySnapshot retrievePolicySnapshot(String policyId);

    /**
     * Stores the policy snapshot to persistent storage (database or file system).
     * This operation should be idempotent.
     *
     * @param snapshot The policy snapshot to store
     */
    @ActivityMethod
    void storePolicySnapshot(PolicySnapshot snapshot);

    /**
     * Sends a notification that the policy has been issued.
     * This may include email, webhook, or other notification mechanisms.
     *
     * @param policyId The policy ID that was issued
     */
    @ActivityMethod
    void notifyPolicyIssued(String policyId);
}
