package com.beema.kernel.workflow.claim;

import com.beema.kernel.ai.service.ClaimAnalyzerService;
import com.beema.kernel.domain.claim.Claim;
import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementation of Temporal activities for AI-powered claim analysis
 */
@Component
public class AgentActivitiesImpl implements AgentActivities {

    private static final Logger log = LoggerFactory.getLogger(AgentActivitiesImpl.class);

    private final ClaimAnalyzerService claimAnalyzerService;

    public AgentActivitiesImpl(ClaimAnalyzerService claimAnalyzerService) {
        this.claimAnalyzerService = claimAnalyzerService;
    }

    @Override
    public ClaimAnalyzerService.ClaimAnalysisResult analyzeClaim(Claim claim) {
        log.info("Temporal Activity: Analyzing claim {} with AI", claim.getClaimNumber());

        // Record activity heartbeat for long-running analysis
        Activity.getExecutionContext().heartbeat(claim.getClaimNumber());

        try {
            ClaimAnalyzerService.ClaimAnalysisResult result = claimAnalyzerService.analyzeClaim(claim);

            log.info("AI Analysis Complete - Next Action: {}, Confidence: {}",
                result.getNextAction(), result.getConfidence());

            return result;

        } catch (Exception e) {
            log.error("AI claim analysis failed", e);
            throw Activity.wrap(e);
        }
    }
}
