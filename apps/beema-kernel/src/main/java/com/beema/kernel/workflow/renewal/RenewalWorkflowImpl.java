package com.beema.kernel.workflow.renewal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Implementation of RenewalWorkflow.
 *
 * Workflow Logic:
 * 1. Calculate renewal premium
 * 2. Compare to current premium
 * 3. If increase < 10%: Auto-approve, send email, complete
 * 4. If increase >= 10%: Wait for underwriter review signal
 */
public class RenewalWorkflowImpl implements RenewalWorkflow {

    private static final Logger log = Workflow.getLogger(RenewalWorkflowImpl.class);
    private static final double AUTO_APPROVAL_THRESHOLD = 0.10; // 10%

    // Workflow state
    private String status = "CALCULATING";
    private String policyNumber;
    private Double currentPremium;
    private Double renewalPremium = null;
    private Double premiumIncreasePercent = null;
    private boolean underwriterReviewed = false;
    private boolean underwriterApproved = false;

    // Activities
    private final RenewalRatingActivities ratingActivities = Workflow.newActivityStub(
            RenewalRatingActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .build()
    );

    private final RenewalNotificationActivities notificationActivities = Workflow.newActivityStub(
            RenewalNotificationActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .build()
    );

    private final RenewalPolicyActivities policyActivities = Workflow.newActivityStub(
            RenewalPolicyActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(10))
                    .build()
    );

    @Override
    public String execute(RenewalStartArgs args) {
        this.policyNumber = args.policyNumber();
        this.currentPremium = args.currentPremium();

        log.info("Renewal workflow started for policy: {} (60 days before expiry)", policyNumber);

        // Step 1: Calculate renewal premium
        this.status = "CALCULATING";
        try {
            RenewalQuoteResult quoteResult = ratingActivities.calculateRenewalPremium(
                    args.policyNumber(),
                    args.currentPremium(),
                    args.coverageDetails(),
                    args.claimsHistory()
            );

            this.renewalPremium = quoteResult.renewalPremium();
            this.premiumIncreasePercent = calculateIncreasePercent(currentPremium, renewalPremium);

            log.info("Renewal premium calculated: {} -> {} ({}% increase)",
                    currentPremium, renewalPremium, premiumIncreasePercent);

        } catch (Exception e) {
            log.error("Renewal calculation failed", e);
            this.status = "CALCULATION_FAILED";
            return this.status;
        }

        // Step 2: Check if auto-approval applies
        if (premiumIncreasePercent < AUTO_APPROVAL_THRESHOLD) {
            log.info("Premium increase {}% < 10% threshold, auto-approving", premiumIncreasePercent * 100);
            return autoApproveRenewal(args);
        } else {
            log.info("Premium increase {}% >= 10% threshold, requiring underwriter review", premiumIncreasePercent * 100);
            return requireUnderwriterReview(args);
        }
    }

    private String autoApproveRenewal(RenewalStartArgs args) {
        this.status = "AUTO_APPROVED";

        // Send auto-approval email
        try {
            notificationActivities.sendRenewalEmail(
                    args.policyNumber(),
                    args.insuredEmail(),
                    this.renewalPremium,
                    "Your policy has been automatically renewed with a premium increase of " +
                            String.format("%.2f%%", premiumIncreasePercent * 100)
            );
            log.info("Auto-approval email sent to: {}", args.insuredEmail());

        } catch (Exception e) {
            log.warn("Email notification failed, but renewal continues", e);
        }

        // Create renewal policy version
        try {
            policyActivities.createRenewalVersion(
                    args.policyNumber(),
                    this.renewalPremium,
                    args.newExpiryDate()
            );

            this.status = "COMPLETED";
            log.info("Renewal completed automatically for policy: {}", policyNumber);

        } catch (Exception e) {
            log.error("Renewal policy creation failed", e);
            this.status = "RENEWAL_FAILED";
        }

        return this.status;
    }

    private String requireUnderwriterReview(RenewalStartArgs args) {
        this.status = "PENDING_REVIEW";

        // Send notification to underwriting team
        try {
            notificationActivities.sendUnderwriterNotification(
                    args.policyNumber(),
                    this.currentPremium,
                    this.renewalPremium,
                    this.premiumIncreasePercent
            );
            log.info("Underwriter notification sent for policy: {}", policyNumber);

        } catch (Exception e) {
            log.warn("Underwriter notification failed", e);
        }

        // Wait indefinitely for underwriter review signal
        log.info("Waiting for underwriter review signal");
        Workflow.await(() -> underwriterReviewed);

        if (!underwriterApproved) {
            log.info("Renewal declined by underwriter for policy: {}", policyNumber);
            this.status = "DECLINED";

            // Send decline notification
            try {
                notificationActivities.sendRenewalDeclinedEmail(
                        args.policyNumber(),
                        args.insuredEmail()
                );
            } catch (Exception e) {
                log.warn("Decline email notification failed", e);
            }

            return this.status;
        }

        // Underwriter approved - create renewal
        log.info("Renewal approved by underwriter for policy: {}", policyNumber);
        this.status = "APPROVED";

        try {
            policyActivities.createRenewalVersion(
                    args.policyNumber(),
                    this.renewalPremium,
                    args.newExpiryDate()
            );

            // Send approval email
            notificationActivities.sendRenewalEmail(
                    args.policyNumber(),
                    args.insuredEmail(),
                    this.renewalPremium,
                    "Your policy renewal has been approved by our underwriting team."
            );

            this.status = "COMPLETED";
            log.info("Renewal completed with underwriter approval for policy: {}", policyNumber);

        } catch (Exception e) {
            log.error("Renewal policy creation failed", e);
            this.status = "RENEWAL_FAILED";
        }

        return this.status;
    }

    @Override
    public void underwriterReview(boolean approved, Double adjustedPremium, String notes) {
        log.info("Underwriter review received: approved={}, adjustedPremium={}, notes={}",
                approved, adjustedPremium, notes);

        this.underwriterReviewed = true;
        this.underwriterApproved = approved;

        if (approved && adjustedPremium != null) {
            log.info("Underwriter adjusted premium: {} -> {}", renewalPremium, adjustedPremium);
            this.renewalPremium = adjustedPremium;
            this.premiumIncreasePercent = calculateIncreasePercent(currentPremium, adjustedPremium);
        }
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Double getRenewalPremium() {
        return renewalPremium;
    }

    @Override
    public Double getPremiumIncreasePercent() {
        return premiumIncreasePercent;
    }

    private double calculateIncreasePercent(double current, double renewal) {
        if (current == 0) return 0;
        return (renewal - current) / current;
    }
}
