package com.beema.kernel.workflow.claim;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import com.beema.kernel.domain.claim.Claim;
import com.beema.kernel.ai.service.ClaimAnalyzerService;

/**
 * Temporal workflow interface for AI-powered claim processing
 */
@WorkflowInterface
public interface ClaimWorkflow {

    @WorkflowMethod
    ClaimWorkflowResult processClaimWithAI(Claim claim);

    /**
     * Result of claim workflow execution
     */
    class ClaimWorkflowResult {
        private String claimId;
        private String finalStatus;
        private ClaimAnalyzerService.ClaimAnalysisResult aiAnalysis;

        // Getters and Setters
        public String getClaimId() {
            return claimId;
        }

        public void setClaimId(String claimId) {
            this.claimId = claimId;
        }

        public String getFinalStatus() {
            return finalStatus;
        }

        public void setFinalStatus(String finalStatus) {
            this.finalStatus = finalStatus;
        }

        public ClaimAnalyzerService.ClaimAnalysisResult getAiAnalysis() {
            return aiAnalysis;
        }

        public void setAiAnalysis(ClaimAnalyzerService.ClaimAnalysisResult aiAnalysis) {
            this.aiAnalysis = aiAnalysis;
        }
    }
}
