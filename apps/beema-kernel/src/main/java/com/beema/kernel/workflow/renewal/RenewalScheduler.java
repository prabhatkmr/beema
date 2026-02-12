package com.beema.kernel.workflow.renewal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Scheduled job to trigger renewal workflows.
 *
 * Runs daily to find policies expiring in 60 days and start renewal workflows.
 */
@Component
public class RenewalScheduler {

    private static final Logger log = LoggerFactory.getLogger(RenewalScheduler.class);
    private static final int RENEWAL_TRIGGER_DAYS = 60;

    private final WorkflowClient workflowClient;
    private final RenewalPolicyFinder policyFinder;

    public RenewalScheduler(WorkflowClient workflowClient, RenewalPolicyFinder policyFinder) {
        this.workflowClient = workflowClient;
        this.policyFinder = policyFinder;
    }

    /**
     * Runs daily at 2 AM to trigger renewal workflows.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void triggerRenewals() {
        log.info("Starting renewal workflow trigger process");

        LocalDateTime triggerDate = LocalDateTime.now().plusDays(RENEWAL_TRIGGER_DAYS);

        // Find policies expiring 60 days from now
        List<RenewalPolicyInfo> policiesToRenew = policyFinder.findPoliciesExpiringOn(triggerDate);

        log.info("Found {} policies expiring on {} (60 days from now)",
                policiesToRenew.size(), triggerDate.toLocalDate());

        for (RenewalPolicyInfo policyInfo : policiesToRenew) {
            try {
                startRenewalWorkflow(policyInfo);
            } catch (Exception e) {
                log.error("Failed to start renewal workflow for policy: {}", policyInfo.policyNumber(), e);
            }
        }

        log.info("Renewal workflow trigger process completed");
    }

    private void startRenewalWorkflow(RenewalPolicyInfo policyInfo) {
        String workflowId = "renewal-" + policyInfo.policyNumber();

        // Check if renewal workflow already exists
        try {
            RenewalWorkflow existingWorkflow = workflowClient.newWorkflowStub(
                    RenewalWorkflow.class,
                    workflowId
            );
            String status = existingWorkflow.getStatus();
            log.info("Renewal workflow already exists for policy {}: status={}",
                    policyInfo.policyNumber(), status);
            return;
        } catch (Exception e) {
            // Workflow doesn't exist, proceed to create
        }

        // Create new renewal workflow
        RenewalWorkflow workflow = workflowClient.newWorkflowStub(
                RenewalWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue("POLICY_TASK_QUEUE")
                        .build()
        );

        RenewalStartArgs args = new RenewalStartArgs(
                policyInfo.policyNumber(),
                policyInfo.currentPremium(),
                policyInfo.expiryDate(),
                policyInfo.expiryDate().plusYears(1),
                policyInfo.coverageDetails(),
                policyInfo.claimsHistory(),
                policyInfo.insuredEmail()
        );

        // Start workflow asynchronously
        WorkflowClient.start(workflow::execute, args);

        log.info("Started renewal workflow for policy: {} (expiry: {})",
                policyInfo.policyNumber(), policyInfo.expiryDate());
    }
}
