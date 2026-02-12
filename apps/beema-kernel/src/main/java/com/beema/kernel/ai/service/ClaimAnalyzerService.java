package com.beema.kernel.ai.service;

import com.beema.kernel.ai.tools.InsuranceTools;
import com.beema.kernel.domain.claim.Claim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI-powered claim analysis service using Spring AI with OpenRouter
 */
@Service
public class ClaimAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(ClaimAnalyzerService.class);

    private final ChatClient chatClient;
    private final InsuranceTools insuranceTools;

    @Value("${beema.ai.model:openai/gpt-4-turbo-preview}")
    private String modelName;

    public ClaimAnalyzerService(ChatClient.Builder chatClientBuilder,
                                InsuranceTools insuranceTools) {
        this.chatClient = chatClientBuilder.build();
        this.insuranceTools = insuranceTools;
    }

    public ClaimAnalysisResult analyzeClaim(Claim claim) {
        log.info("Analyzing claim {} using OpenRouter with model: {}",
            claim.getClaimNumber(), modelName);

        try {
            String prompt = buildClaimAnalysisPrompt(claim);

            // Note: Spring AI function calling requires specific setup
            // For now, use simple prompt-based analysis without function calling
            var response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

            return parseAnalysisResult(response, claim);

        } catch (Exception e) {
            log.error("AI analysis failed for claim {} with model {}: {}",
                claim.getClaimNumber(), modelName, e.getMessage());
            return createFallbackResult(claim, e);
        }
    }

    private String buildClaimAnalysisPrompt(Claim claim) {
        return String.format("""
            You are an expert insurance claims analyst for the Beema platform.

            Analyze the following claim and provide a recommended next action for the claims handler.

            **Claim Details:**
            - Claim Number: %s
            - Claim Type: %s
            - Policy Number: %s
            - Claim Amount: %.2f
            - Status: %s
            - Market Context: %s
            - Description: %s
            - Incident Date: %s

            **Your Analysis Should:**
            1. Use the available tools to validate the claim against business rules
            2. Calculate relevant claim metrics (claim ratio, severity)
            3. Check if the claim amount exceeds policy limits
            4. Identify any missing required fields
            5. Recommend one of these next actions:
               - APPROVE_IMMEDIATELY (straightforward, low-value, all rules pass)
               - REQUEST_DOCUMENTS (missing information or supporting documents needed)
               - ESCALATE_TO_SPECIALIST (high-value, complex, or unusual circumstances)
               - REFER_TO_INVESTIGATOR (fraud indicators, suspicious patterns)
               - REJECT (clear policy violation or exclusion applies)

            **Output Format:**
            Provide your response in this exact JSON structure:
            {
              "nextAction": "ACTION_NAME",
              "confidence": 0.95,
              "reasoning": "Detailed explanation of your decision",
              "requiredDocuments": ["list", "of", "documents"],
              "warnings": ["any", "warnings"],
              "estimatedSettlement": 0.0
            }
            """,
            claim.getClaimNumber(),
            claim.getClaimType(),
            claim.getPolicyNumber(),
            claim.getClaimAmount(),
            claim.getStatus(),
            claim.getMarketContext(),
            claim.getDescription(),
            claim.getIncidentDate()
        );
    }

    private ClaimAnalysisResult parseAnalysisResult(String aiResponse, Claim claim) {
        // Parse AI response and create result object
        ClaimAnalysisResult result = new ClaimAnalysisResult();
        result.setClaimId(claim.getClaimId());
        result.setAiAnalysis(aiResponse);

        // Extract action from response (simple pattern matching)
        if (aiResponse.contains("APPROVE_IMMEDIATELY")) {
            result.setNextAction("APPROVE_IMMEDIATELY");
            result.setConfidence(0.9);
        } else if (aiResponse.contains("REQUEST_DOCUMENTS")) {
            result.setNextAction("REQUEST_DOCUMENTS");
            result.setConfidence(0.85);
        } else if (aiResponse.contains("ESCALATE_TO_SPECIALIST")) {
            result.setNextAction("ESCALATE_TO_SPECIALIST");
            result.setConfidence(0.8);
        } else if (aiResponse.contains("REFER_TO_INVESTIGATOR")) {
            result.setNextAction("REFER_TO_INVESTIGATOR");
            result.setConfidence(0.75);
        } else if (aiResponse.contains("REJECT")) {
            result.setNextAction("REJECT");
            result.setConfidence(0.9);
        } else {
            result.setNextAction("MANUAL_REVIEW");
            result.setConfidence(0.5);
        }

        result.setReasoning(extractReasoning(aiResponse));

        return result;
    }

    private String extractReasoning(String aiResponse) {
        // Simple extraction - in production use proper JSON parsing
        int reasoningStart = aiResponse.indexOf("reasoning");
        if (reasoningStart > 0) {
            int reasoningEnd = Math.min(reasoningStart + 500, aiResponse.length());
            return aiResponse.substring(reasoningStart, reasoningEnd);
        }
        return aiResponse.substring(0, Math.min(500, aiResponse.length()));
    }

    private ClaimAnalysisResult createFallbackResult(Claim claim, Exception error) {
        ClaimAnalysisResult result = new ClaimAnalysisResult();
        result.setClaimId(claim.getClaimId());
        result.setNextAction("MANUAL_REVIEW");
        result.setConfidence(0.0);
        result.setReasoning("AI analysis failed: " + error.getMessage() + ". Requires manual review.");
        result.setAiAnalysis("ERROR: " + error.getMessage());
        return result;
    }

    /**
     * Result of AI claim analysis
     */
    public static class ClaimAnalysisResult {
        private String claimId;
        private String nextAction;
        private Double confidence;
        private String reasoning;
        private String aiAnalysis;

        // Getters and Setters
        public String getClaimId() {
            return claimId;
        }

        public void setClaimId(String claimId) {
            this.claimId = claimId;
        }

        public String getNextAction() {
            return nextAction;
        }

        public void setNextAction(String nextAction) {
            this.nextAction = nextAction;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public String getReasoning() {
            return reasoning;
        }

        public void setReasoning(String reasoning) {
            this.reasoning = reasoning;
        }

        public String getAiAnalysis() {
            return aiAnalysis;
        }

        public void setAiAnalysis(String aiAnalysis) {
            this.aiAnalysis = aiAnalysis;
        }
    }
}
