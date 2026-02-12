package com.beema.kernel.workflow.renewal;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Durable workflow for policy renewal processing.
 *
 * Triggered: 60 days before policy expiration
 * Logic:
 *   1. Auto-calculate new premium
 *   2. If increase < 10%: Auto-approve and send email
 *   3. If increase >= 10%: Wait for UNDERWRITER_REVIEW signal
 */
@WorkflowInterface
public interface RenewalWorkflow {

    /**
     * Main workflow entry point.
     * Starts 60 days before policy expiry.
     *
     * @param args Renewal workflow arguments
     * @return Final renewal status
     */
    @WorkflowMethod
    String execute(RenewalStartArgs args);

    /**
     * Signal for underwriter approval.
     * Required when premium increase >= 10%.
     *
     * @param approved Whether renewal is approved
     * @param adjustedPremium Optional adjusted premium (if underwriter modified)
     * @param notes Underwriter notes
     */
    @SignalMethod
    void underwriterReview(boolean approved, Double adjustedPremium, String notes);

    /**
     * Query current renewal status.
     *
     * @return Current status (CALCULATING, PENDING_REVIEW, APPROVED, DECLINED, COMPLETED)
     */
    @QueryMethod
    String getStatus();

    /**
     * Query calculated renewal premium.
     *
     * @return Renewal premium amount
     */
    @QueryMethod
    Double getRenewalPremium();

    /**
     * Query premium increase percentage.
     *
     * @return Percentage increase from current premium
     */
    @QueryMethod
    Double getPremiumIncreasePercent();
}
