package com.beema.kernel.workflow.claim;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import com.beema.kernel.domain.claim.Claim;
import com.beema.kernel.ai.service.ClaimAnalyzerService;

/**
 * Temporal activities for AI-powered claim processing
 */
@ActivityInterface
public interface AgentActivities {

    @ActivityMethod
    ClaimAnalyzerService.ClaimAnalysisResult analyzeClaim(Claim claim);
}
