package com.beema.kernel.workflow.claim;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Map;

/**
 * Implementation of the ClaimWorkflow with human-in-the-loop approval.
 *
 * Flow:
 *   1. Validate claim data (automated)
 *   2. Create a human task in the inbox via TaskActivities
 *   3. Pause execution with Workflow.await() until HumanApproval signal
 *   4. Process the outcome (APPROVED → settle, REJECTED → close)
 *
 * The workflow is deterministic. State (approvalOutcome) is replayed
 * correctly on workflow recovery.
 */
public class ClaimWorkflowImpl implements ClaimWorkflow {

    private static final Logger log = Workflow.getLogger(ClaimWorkflowImpl.class);

    private final TaskActivities taskActivities = Workflow.newActivityStub(
            TaskActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(1))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(5))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build()
    );

    private String status;
    private String approvalOutcome;

    @Override
    public String execute(String claimId, String tenantId, Map<String, Object> claimData) {
        // ----- Step 1: Automated Validation -----
        status = "SUBMITTED";
        log.info("Claim {} submitted for tenant {}", claimId, tenantId);

        double claimAmount = claimData.containsKey("claimAmount")
                ? ((Number) claimData.get("claimAmount")).doubleValue()
                : 0.0;
        String claimType = (String) claimData.getOrDefault("claimType", "UNKNOWN");

        log.info("Claim {} validated: type={}, amount={}", claimId, claimType, claimAmount);

        // ----- Step 2: Create human approval task -----
        status = "PENDING_APPROVAL";
        String workflowId = Workflow.getInfo().getWorkflowId();
        String runId = Workflow.getInfo().getRunId();

        taskActivities.createHumanTask(
                workflowId,
                runId,
                tenantId,
                "CLAIMS_ADJUSTER",
                "APPROVAL",
                "Approve Claim " + claimId,
                String.format("Claim %s (%s) for amount %.2f requires adjuster approval.",
                        claimId, claimType, claimAmount),
                "HumanApproval"
        );

        log.info("Claim {} waiting for human approval (workflow={}, run={})",
                claimId, workflowId, runId);

        // ----- Step 3: Pause and wait for human signal -----
        Workflow.await(() -> approvalOutcome != null);

        // ----- Step 4: Process outcome -----
        log.info("Claim {} received approval outcome: {}", claimId, approvalOutcome);

        if ("APPROVED".equalsIgnoreCase(approvalOutcome)) {
            status = "APPROVED";
            log.info("Claim {} approved. Proceeding to settlement.", claimId);

            // Here you would call settlement activities, payment processing, etc.
            // e.g., settlementActivities.initatePayment(claimId, claimAmount);

            status = "SETTLED";
            log.info("Claim {} settled successfully", claimId);
        } else {
            status = "REJECTED";
            log.info("Claim {} rejected by adjuster", claimId);
        }

        return status;
    }

    @Override
    public void humanApproval(String outcome) {
        log.info("HumanApproval signal received with outcome: {}", outcome);
        this.approvalOutcome = outcome;
    }
}
