package com.beema.kernel.workflow.claim;

import com.beema.kernel.ai.service.ClaimAnalyzerService;
import com.beema.kernel.domain.claim.Claim;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

/**
 * Implementation of Temporal workflow for AI-powered claim processing
 */
public class ClaimWorkflowImpl implements ClaimWorkflow {

    private final AgentActivities activities;

    public ClaimWorkflowImpl() {
        this.activities = Workflow.newActivityStub(
            AgentActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .setRetryOptions(
                    io.temporal.common.RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofSeconds(10))
                        .build()
                )
                .build()
        );
    }

    @Override
    public ClaimWorkflowResult processClaimWithAI(Claim claim) {
        // Step 1: AI Analysis
        ClaimAnalyzerService.ClaimAnalysisResult aiAnalysis = activities.analyzeClaim(claim);

        // Step 2: Determine workflow path based on AI recommendation
        String finalStatus;
        switch (aiAnalysis.getNextAction()) {
            case "APPROVE_IMMEDIATELY":
                finalStatus = "APPROVED";
                break;
            case "REJECT":
                finalStatus = "REJECTED";
                break;
            case "REQUEST_DOCUMENTS":
                finalStatus = "PENDING_DOCUMENTS";
                break;
            case "ESCALATE_TO_SPECIALIST":
                finalStatus = "ESCALATED";
                break;
            case "REFER_TO_INVESTIGATOR":
                finalStatus = "UNDER_INVESTIGATION";
                break;
            default:
                finalStatus = "MANUAL_REVIEW";
        }

        ClaimWorkflowResult result = new ClaimWorkflowResult();
        result.setClaimId(claim.getClaimId());
        result.setFinalStatus(finalStatus);
        result.setAiAnalysis(aiAnalysis);

        return result;
    }
}
