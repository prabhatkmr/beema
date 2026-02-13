package com.beema.kernel.workflow.claim;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Temporal workflow interface for claim processing with human-in-the-loop approval.
 *
 * Lifecycle:
 *   1. SUBMITTED  - Claim received, automated validation runs
 *   2. PENDING    - Human task created, workflow pauses waiting for approval signal
 *   3. APPROVED   - Adjuster approved the claim via the task inbox
 *      REJECTED   - Adjuster rejected the claim
 *   4. SETTLED    - Approved claim finalized (payment initiated, etc.)
 *
 * The workflow pauses at step 2 using Workflow.await() and resumes
 * when the TaskService bridge sends a "HumanApproval" signal after
 * the user completes the task via POST /api/v1/tasks/{id}/complete.
 */
@WorkflowInterface
public interface ClaimWorkflow {

    /**
     * Execute the claim workflow.
     *
     * @param claimId   unique identifier for this claim
     * @param tenantId  tenant context
     * @param claimData claim details (amount, type, description, etc.)
     * @return final status of the claim (APPROVED, REJECTED)
     */
    @WorkflowMethod
    String execute(String claimId, String tenantId, java.util.Map<String, Object> claimData);

    /**
     * Signal from the bridge service when a human completes the approval task.
     * Carries the outcome: "APPROVED" or "REJECTED".
     */
    @SignalMethod(name = "HumanApproval")
    void humanApproval(String outcome);
}
