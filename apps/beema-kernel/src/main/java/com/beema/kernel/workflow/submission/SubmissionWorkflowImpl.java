package com.beema.kernel.workflow.submission;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Implementation of SubmissionWorkflow.
 *
 * State Machine:
 * DRAFT → [quote() signal] → QUOTED → [bind() signal] → BOUND
 *
 * The workflow waits indefinitely for signals, making it suitable for
 * human-in-the-loop processes where quote approval may take days.
 */
public class SubmissionWorkflowImpl implements SubmissionWorkflow {

    private static final Logger log = Workflow.getLogger(SubmissionWorkflowImpl.class);

    // Workflow state (persisted by Temporal)
    private String status = "DRAFT";
    private String submissionId;
    private Double quotedPremium = null;
    private String policyNumber = null;

    // Signal flags
    private boolean quoteRequested = false;
    private boolean bindRequested = false;

    // Activities
    private final RatingEngineActivities ratingEngine = Workflow.newActivityStub(
            RatingEngineActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .build()
    );

    private final PolicyCreationActivities policyCreation = Workflow.newActivityStub(
            PolicyCreationActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(10))
                    .build()
    );

    @Override
    public String execute(SubmissionStartArgs args) {
        this.submissionId = args.submissionId();
        log.info("Submission workflow started: {}", submissionId);

        // State: DRAFT
        this.status = "DRAFT";
        log.info("Submission in DRAFT state, waiting for quote signal");

        // Wait indefinitely for quote signal
        Workflow.await(() -> quoteRequested);

        // State: Processing Quote
        log.info("Quote signal received, calculating premium");
        try {
            QuoteResult quoteResult = ratingEngine.calculatePremium(
                    args.submissionId(),
                    args.productType(),
                    args.coverageDetails(),
                    args.riskFactors()
            );

            this.quotedPremium = quoteResult.premium();
            this.status = "QUOTED";
            log.info("Quote calculated: {} - Premium: {}", submissionId, quotedPremium);

        } catch (Exception e) {
            log.error("Quote calculation failed", e);
            this.status = "QUOTE_FAILED";
            return this.status;
        }

        // Wait indefinitely for bind signal
        log.info("Submission quoted, waiting for bind signal");
        Workflow.await(() -> bindRequested);

        // State: Binding
        log.info("Bind signal received, creating policy");
        try {
            PolicyCreationResult policyResult = policyCreation.createPolicy(
                    args.submissionId(),
                    this.quotedPremium,
                    args.coverageDetails(),
                    args.effectiveDate(),
                    args.expiryDate()
            );

            this.policyNumber = policyResult.policyNumber();
            this.status = "BOUND";
            log.info("Policy created: {} for submission: {}", policyNumber, submissionId);

        } catch (Exception e) {
            log.error("Policy creation failed", e);
            this.status = "BIND_FAILED";
            return this.status;
        }

        return this.status;
    }

    @Override
    public void quote() {
        if (!"DRAFT".equals(status)) {
            log.warn("Cannot quote submission in status: {}", status);
            return;
        }
        log.info("Quote signal received for submission: {}", submissionId);
        this.quoteRequested = true;
    }

    @Override
    public void bind() {
        if (!"QUOTED".equals(status)) {
            log.warn("Cannot bind submission in status: {}", status);
            return;
        }
        log.info("Bind signal received for submission: {}", submissionId);
        this.bindRequested = true;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Double getQuotedPremium() {
        return quotedPremium;
    }

    @Override
    public String getPolicyNumber() {
        return policyNumber;
    }
}
