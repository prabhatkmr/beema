package com.beema.kernel.workflow.policy;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of PolicyLifecycleWorkflow.
 *
 * State Management:
 * - currentStatus: Policy status (ACTIVE, CANCELLED, EXPIRED)
 * - activeVersion: Current policy version number
 * - policyId: Unique policy identifier
 * - expiryDate: Policy expiration date
 *
 * Signal Handling:
 * - Signals are processed asynchronously
 * - Workflow sleeps until expiry or signal
 * - Can handle multiple signals during lifecycle
 */
public class PolicyLifecycleWorkflowImpl implements PolicyLifecycleWorkflow {

    private static final Logger log = Workflow.getLogger(PolicyLifecycleWorkflowImpl.class);

    // Workflow state (persisted)
    private String currentStatus = "DRAFT";
    private Integer activeVersion = 0;
    private String policyId;
    private LocalDateTime inceptionDate;
    private LocalDateTime expiryDate;
    private List<String> events = new ArrayList<>();

    // Scheduled events
    private boolean cancelled = false;
    private LocalDateTime cancellationDate = null;

    // Activities
    private final PersistenceActivities persistenceActivities = Workflow.newActivityStub(
            PersistenceActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .build()
    );

    private final RatingActivities ratingActivities = Workflow.newActivityStub(
            RatingActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .build()
    );

    @Override
    public String execute(PolicyStartArgs args) {
        log.info("Starting policy lifecycle for policy: {}", args.policyNumber());

        this.policyId = args.policyNumber();
        this.inceptionDate = args.inceptionDate();
        this.expiryDate = args.expiryDate();

        // Step 1: Create initial policy version
        PolicyVersionResult result = persistenceActivities.createPolicyVersion(
                args.policyNumber(),
                args.inceptionDate(),
                args.expiryDate(),
                args.premium(),
                args.coverageDetails()
        );

        this.activeVersion = result.version();
        this.currentStatus = "ACTIVE";
        events.add("POLICY_CREATED");

        log.info("Policy created: {} - Version: {}", policyId, activeVersion);

        // Step 2: Sleep until expiry (or until signaled)
        try {
            long nowMs = Workflow.currentTimeMillis();
            long expiryMs = expiryDate.toInstant(ZoneOffset.UTC).toEpochMilli();
            Duration untilExpiry = Duration.ofMillis(expiryMs - nowMs);

            log.info("Policy active until: {} (sleeping for {})", expiryDate, untilExpiry);

            Workflow.await(untilExpiry, () -> cancelled);

            // If we wake up due to cancellation
            if (cancelled) {
                log.info("Policy cancelled on: {}", cancellationDate);
                currentStatus = "CANCELLED";
                events.add("POLICY_CANCELLED");
                return currentStatus;
            }

            // Otherwise, policy expired naturally
            log.info("Policy expired on: {}", expiryDate);
            currentStatus = "EXPIRED";
            events.add("POLICY_EXPIRED");

        } catch (Exception e) {
            log.error("Error during policy lifecycle", e);
            currentStatus = "ERROR";
            throw new RuntimeException("Policy lifecycle error: " + e.getMessage(), e);
        }

        return currentStatus;
    }

    @Override
    public void endorse(EndorsementArgs args) {
        log.info("Processing endorsement for policy: {} on {}", policyId, args.effectiveDate());

        if (!"ACTIVE".equals(currentStatus)) {
            log.warn("Cannot endorse non-active policy. Status: {}", currentStatus);
            return;
        }

        // Step 1: Calculate pro-rata adjustment
        ProRataResult proRata = ratingActivities.calculateProRata(
                args.oldPremium(),
                args.newPremium(),
                args.effectiveDate(),
                expiryDate
        );

        log.info("Pro-rata adjustment: {}", proRata.adjustment());

        // Step 2: Create endorsement version (bitemporal update)
        PolicyVersionResult result = persistenceActivities.createEndorsementVersion(
                policyId,
                args.effectiveDate(),
                args.changes(),
                proRata.adjustment()
        );

        // Update workflow state
        this.activeVersion = result.version();
        events.add("POLICY_ENDORSED");

        log.info("Endorsement created: Version {} effective {}", activeVersion, args.effectiveDate());
    }

    @Override
    public void cancel(CancellationArgs args) {
        log.info("Processing cancellation for policy: {} effective {}", policyId, args.effectiveDate());

        if (!"ACTIVE".equals(currentStatus)) {
            log.warn("Cannot cancel non-active policy. Status: {}", currentStatus);
            return;
        }

        // Calculate refund
        ProRataResult refund = ratingActivities.calculateProRata(
                args.currentPremium(),
                0.0,
                args.effectiveDate(),
                expiryDate
        );

        log.info("Cancellation refund: {}", refund.adjustment());

        // Create cancellation version
        persistenceActivities.createCancellationVersion(
                policyId,
                args.effectiveDate(),
                args.reason(),
                refund.adjustment()
        );

        // Schedule cancellation
        this.cancelled = true;
        this.cancellationDate = args.effectiveDate();
        events.add("CANCELLATION_SCHEDULED");

        log.info("Cancellation scheduled for: {}", args.effectiveDate());
    }

    @Override
    public void renew(RenewalArgs args) {
        log.info("Processing renewal for policy: {}", policyId);

        if (!"ACTIVE".equals(currentStatus) && !"EXPIRED".equals(currentStatus)) {
            log.warn("Cannot renew policy in status: {}", currentStatus);
            return;
        }

        // Create renewal version
        PolicyVersionResult result = persistenceActivities.createPolicyVersion(
                policyId + "-R" + (activeVersion + 1),
                args.newInceptionDate(),
                args.newExpiryDate(),
                args.newPremium(),
                args.coverageDetails()
        );

        // Update workflow state
        this.activeVersion = result.version();
        this.currentStatus = "ACTIVE";
        this.inceptionDate = args.newInceptionDate();
        this.expiryDate = args.newExpiryDate();
        events.add("POLICY_RENEWED");

        log.info("Policy renewed: New expiry {}", expiryDate);
    }

    @Override
    public String getCurrentStatus() {
        return currentStatus;
    }

    @Override
    public Integer getActiveVersion() {
        return activeVersion;
    }
}
