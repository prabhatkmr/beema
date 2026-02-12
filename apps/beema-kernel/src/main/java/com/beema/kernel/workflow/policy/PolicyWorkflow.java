package com.beema.kernel.workflow.policy;

import com.beema.kernel.workflow.policy.model.PolicyWorkflowRequest;
import com.beema.kernel.workflow.policy.model.PolicyWorkflowResult;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Policy Workflow Interface
 *
 * Manages the lifecycle of insurance policies, handling state transitions
 * from SUBMITTED through ISSUED states with validation, snapshotting, and notifications.
 */
@WorkflowInterface
public interface PolicyWorkflow {

    /**
     * Processes the complete policy lifecycle from submission through issuance.
     *
     * @param request Policy workflow request containing policy ID, initial state, and metadata
     * @return PolicyWorkflowResult containing final state, snapshot ID, and timestamp
     */
    @WorkflowMethod
    PolicyWorkflowResult processPolicyLifecycle(PolicyWorkflowRequest request);

    /**
     * Signal method to update the policy state during workflow execution.
     *
     * @param newState The new state to transition to (e.g., "ISSUED", "CANCELLED")
     */
    @SignalMethod
    void updateState(String newState);

    /**
     * Query method to retrieve the current state of the policy workflow.
     *
     * @return Current state of the policy
     */
    @QueryMethod
    String getCurrentState();
}
