package com.beema.kernel.workflow.policy;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Durable workflow for managing the complete lifecycle of an insurance policy.
 *
 * Lifecycle events:
 * - Creation (execute)
 * - Endorsement (signal)
 * - Cancellation (signal)
 * - Renewal (signal)
 *
 * The workflow maintains policy state and automatically handles time-based
 * events like expiry and scheduled endorsements.
 */
@WorkflowInterface
public interface PolicyLifecycleWorkflow {

    /**
     * Main workflow entry point.
     * Creates the initial policy version and manages its lifecycle.
     *
     * @param args Policy creation arguments
     * @return Final policy status when workflow completes
     */
    @WorkflowMethod
    String execute(PolicyStartArgs args);

    /**
     * Signal to endorse the policy (mid-term adjustment).
     *
     * Creates a new bitemporal version with updated attributes.
     *
     * @param args Endorsement details (effective date, changes)
     */
    @SignalMethod
    void endorse(EndorsementArgs args);

    /**
     * Signal to cancel the policy.
     *
     * Can be immediate or scheduled for future date.
     * Calculates pro-rata refund if applicable.
     *
     * @param args Cancellation details (effective date, reason)
     */
    @SignalMethod
    void cancel(CancellationArgs args);

    /**
     * Signal to renew the policy.
     *
     * Creates new policy term with updated premium/coverage.
     *
     * @param args Renewal details (new term dates, premium)
     */
    @SignalMethod
    void renew(RenewalArgs args);

    /**
     * Query current policy status.
     *
     * @return Current status (ACTIVE, CANCELLED, EXPIRED, etc.)
     */
    @QueryMethod
    String getCurrentStatus();

    /**
     * Query active policy version.
     *
     * @return Version number of currently active policy
     */
    @QueryMethod
    Integer getActiveVersion();
}
