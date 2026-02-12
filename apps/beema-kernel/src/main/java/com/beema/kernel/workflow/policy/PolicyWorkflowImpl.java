package com.beema.kernel.workflow.policy;

import com.beema.kernel.workflow.policy.model.PolicySnapshot;
import com.beema.kernel.workflow.policy.model.PolicyWorkflowRequest;
import com.beema.kernel.workflow.policy.model.PolicyWorkflowResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Policy Workflow Implementation
 *
 * Manages the complete lifecycle of a policy from SUBMITTED to ISSUED state.
 * Handles validation, snapshotting, storage, and notifications with retry logic.
 */
public class PolicyWorkflowImpl implements PolicyWorkflow {

    private static final Logger log = Workflow.getLogger(PolicyWorkflowImpl.class);

    private String currentState;
    private PolicySnapshot currentSnapshot;

    // Configure retry options for activities
    private static final RetryOptions RETRY_OPTIONS = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(30))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(5)
            .build();

    // Configure activity options with timeouts and retry
    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(2))
            .setScheduleToCloseTimeout(Duration.ofMinutes(5))
            .setRetryOptions(RETRY_OPTIONS)
            .build();

    // Create activity stub with options
    private final PolicySnapshotActivity activities =
            Workflow.newActivityStub(PolicySnapshotActivity.class, ACTIVITY_OPTIONS);

    @Override
    public PolicyWorkflowResult processPolicyLifecycle(PolicyWorkflowRequest request) {
        log.info("Starting policy lifecycle workflow: policyId={}, initialState={}",
                request.getPolicyId(), request.getInitialState());

        // Initialize workflow state
        this.currentState = request.getInitialState();

        try {
            // Process based on initial state
            if ("SUBMITTED".equals(currentState)) {
                return handleSubmittedState(request);
            } else {
                log.warn("Unexpected initial state: {}", currentState);
                return PolicyWorkflowResult.failure(
                        currentState,
                        "Workflow started with unexpected state: " + currentState
                );
            }

        } catch (Exception e) {
            log.error("Policy workflow failed: policyId={}, error={}",
                    request.getPolicyId(), e.getMessage(), e);
            return PolicyWorkflowResult.failure(
                    currentState,
                    "Workflow failed: " + e.getMessage()
            );
        }
    }

    /**
     * Handles the SUBMITTED state transition.
     * Steps:
     * 1. Validate policy data
     * 2. Retrieve policy snapshot (retryable activity)
     * 3. Transition to ISSUED state
     * 4. Store snapshot
     * 5. Send notifications
     */
    private PolicyWorkflowResult handleSubmittedState(PolicyWorkflowRequest request) {
        log.info("Processing SUBMITTED state for policyId: {}", request.getPolicyId());

        // Step 1: Validate policy data
        if (!validatePolicyData(request)) {
            log.error("Policy validation failed for policyId: {}", request.getPolicyId());
            return PolicyWorkflowResult.failure(
                    "VALIDATION_FAILED",
                    "Policy data validation failed"
            );
        }

        // Step 2: Execute retryable activity to retrieve policy snapshot
        try {
            log.info("Retrieving policy snapshot for policyId: {}", request.getPolicyId());
            this.currentSnapshot = activities.retrievePolicySnapshot(request.getPolicyId());
            log.info("Policy snapshot retrieved successfully: snapshotId={}",
                    currentSnapshot.getSnapshotId());

        } catch (Exception e) {
            log.error("Failed to retrieve policy snapshot after retries: policyId={}",
                    request.getPolicyId(), e);
            return PolicyWorkflowResult.failure(
                    "SUBMITTED",
                    "Failed to retrieve policy snapshot: " + e.getMessage()
            );
        }

        // Step 3: Transition to ISSUED state
        this.currentState = "ISSUED";
        log.info("Policy state transitioned to ISSUED: policyId={}", request.getPolicyId());

        // Step 4: Handle ISSUED state
        return handleIssuedState(request);
    }

    /**
     * Handles the ISSUED state.
     * Steps:
     * 1. Capture and store policy snapshot
     * 2. Store policy document
     * 3. Notify relevant parties
     */
    private PolicyWorkflowResult handleIssuedState(PolicyWorkflowRequest request) {
        log.info("Processing ISSUED state for policyId: {}", request.getPolicyId());

        try {
            // Step 1: Store the policy snapshot
            if (currentSnapshot != null) {
                currentSnapshot.setState("ISSUED");
                log.info("Storing policy snapshot: snapshotId={}", currentSnapshot.getSnapshotId());
                activities.storePolicySnapshot(currentSnapshot);
                log.info("Policy snapshot stored successfully");
            } else {
                log.warn("No snapshot available to store for policyId: {}", request.getPolicyId());
            }

            // Step 2: Notify relevant parties
            log.info("Sending policy issued notification for policyId: {}", request.getPolicyId());
            activities.notifyPolicyIssued(request.getPolicyId());
            log.info("Policy issued notification sent successfully");

            // Step 3: Return success result
            log.info("Policy workflow completed successfully: policyId={}, snapshotId={}",
                    request.getPolicyId(), currentSnapshot.getSnapshotId());

            return PolicyWorkflowResult.success(
                    "ISSUED",
                    currentSnapshot != null ? currentSnapshot.getSnapshotId() : null
            );

        } catch (Exception e) {
            log.error("Failed to complete ISSUED state processing: policyId={}",
                    request.getPolicyId(), e);
            return PolicyWorkflowResult.failure(
                    "ISSUED",
                    "Failed to complete policy issuance: " + e.getMessage()
            );
        }
    }

    /**
     * Validates policy data before processing.
     * In a real implementation, this would perform comprehensive validation.
     */
    private boolean validatePolicyData(PolicyWorkflowRequest request) {
        // Basic validation
        if (request.getPolicyId() == null || request.getPolicyId().isEmpty()) {
            log.error("Policy ID is null or empty");
            return false;
        }

        if (request.getMetadata() == null) {
            log.error("Policy metadata is null");
            return false;
        }

        // In a real implementation, add more validation:
        // - Schema validation
        // - Business rule validation
        // - Data completeness checks
        // - Regulatory compliance checks

        log.info("Policy validation passed for policyId: {}", request.getPolicyId());
        return true;
    }

    @Override
    public void updateState(String newState) {
        log.info("Received signal to update state: currentState={}, newState={}",
                this.currentState, newState);
        this.currentState = newState;
        log.info("State updated successfully to: {}", newState);
    }

    @Override
    public String getCurrentState() {
        return this.currentState;
    }
}
