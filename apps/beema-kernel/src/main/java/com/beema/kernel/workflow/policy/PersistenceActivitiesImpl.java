package com.beema.kernel.workflow.policy;

import com.beema.kernel.domain.policy.Policy;
import com.beema.kernel.repository.policy.PolicyRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of PersistenceActivities with SCD Type 2 bitemporal logic.
 *
 * SCD Type 2 ensures:
 * - Full audit history (no updates to data, only inserts of new versions)
 * - Point-in-time queries via valid_from/valid_to ranges
 * - Clear version lineage via version number
 */
@Component
public class PersistenceActivitiesImpl implements PersistenceActivities {

    private final PolicyRepository policyRepository;

    public PersistenceActivitiesImpl(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Override
    @Transactional
    public PolicyVersionResult createPolicyVersion(
            String policyNumber,
            LocalDateTime inceptionDate,
            LocalDateTime expiryDate,
            Double premium,
            Map<String, Object> coverageDetails) {

        Policy policy = new Policy();
        policy.setPolicyNumber(policyNumber);
        policy.setVersion(1);
        policy.setStatus("ACTIVE");
        policy.setPremium(premium);
        policy.setInceptionDate(inceptionDate);
        policy.setExpiryDate(expiryDate);
        policy.setCoverageDetails(coverageDetails);

        // Bitemporal fields
        policy.setValidFrom(inceptionDate);
        policy.setValidTo(LocalDateTime.of(9999, 12, 31, 23, 59, 59));
        policy.setTransactionTime(LocalDateTime.now());
        policy.setIsCurrent(true);

        // Audit
        policy.setTenantId("default");
        policy.setCreatedBy("system");
        policy.setUpdatedBy("system");

        Policy saved = policyRepository.save(policy);

        return new PolicyVersionResult(
                saved.getPolicyNumber(),
                saved.getVersion(),
                saved.getStatus()
        );
    }

    @Override
    @Transactional
    public PolicyVersionResult createEndorsementVersion(
            String policyNumber,
            LocalDateTime effectiveDate,
            Map<String, Object> changes,
            Double proRataAdjustment) {

        // Step 1: Get current version
        Policy currentPolicy = policyRepository.findByPolicyNumberAndIsCurrent(policyNumber, true)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyNumber));

        // Step 2: SCD Type 2 — close current version's validity period
        currentPolicy.setValidTo(effectiveDate);
        currentPolicy.setIsCurrent(false);
        currentPolicy.setUpdatedBy("endorsement");
        policyRepository.save(currentPolicy);

        // Step 3: Create new version starting from endorsement date
        Policy newVersion = new Policy();
        newVersion.setPolicyNumber(policyNumber);
        newVersion.setVersion(currentPolicy.getVersion() + 1);
        newVersion.setStatus(currentPolicy.getStatus());

        // Apply premium adjustment
        newVersion.setPremium(currentPolicy.getPremium() + proRataAdjustment);
        if (changes.containsKey("premium")) {
            newVersion.setPremium((Double) changes.get("premium"));
        }

        newVersion.setInceptionDate(currentPolicy.getInceptionDate());
        newVersion.setExpiryDate(currentPolicy.getExpiryDate());
        newVersion.setCoverageDetails(mergeCoverageDetails(currentPolicy.getCoverageDetails(), changes));

        // Bitemporal fields for new version
        newVersion.setValidFrom(effectiveDate);
        newVersion.setValidTo(LocalDateTime.of(9999, 12, 31, 23, 59, 59));
        newVersion.setTransactionTime(LocalDateTime.now());
        newVersion.setIsCurrent(true);

        // Audit
        newVersion.setTenantId(currentPolicy.getTenantId());
        newVersion.setCreatedBy("endorsement");
        newVersion.setUpdatedBy("endorsement");

        Policy saved = policyRepository.save(newVersion);

        return new PolicyVersionResult(
                saved.getPolicyNumber(),
                saved.getVersion(),
                saved.getStatus()
        );
    }

    @Override
    @Transactional
    public void createCancellationVersion(
            String policyNumber,
            LocalDateTime effectiveDate,
            String reason,
            Double refundAmount) {

        // Get current version
        Policy currentPolicy = policyRepository.findByPolicyNumberAndIsCurrent(policyNumber, true)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyNumber));

        // SCD Type 2: close current version
        currentPolicy.setValidTo(effectiveDate);
        currentPolicy.setIsCurrent(false);
        currentPolicy.setUpdatedBy("cancellation");
        policyRepository.save(currentPolicy);

        // Create cancelled version
        Policy cancelledVersion = new Policy();
        cancelledVersion.setPolicyNumber(policyNumber);
        cancelledVersion.setVersion(currentPolicy.getVersion() + 1);
        cancelledVersion.setStatus("CANCELLED");
        cancelledVersion.setPremium(currentPolicy.getPremium());
        cancelledVersion.setInceptionDate(currentPolicy.getInceptionDate());
        cancelledVersion.setExpiryDate(effectiveDate);

        // Add cancellation metadata to coverage details
        Map<String, Object> coverage = new HashMap<>(currentPolicy.getCoverageDetails());
        coverage.put("cancellation_reason", reason);
        coverage.put("refund_amount", refundAmount);
        cancelledVersion.setCoverageDetails(coverage);

        // Bitemporal fields
        cancelledVersion.setValidFrom(effectiveDate);
        cancelledVersion.setValidTo(LocalDateTime.of(9999, 12, 31, 23, 59, 59));
        cancelledVersion.setTransactionTime(LocalDateTime.now());
        cancelledVersion.setIsCurrent(true);

        // Audit
        cancelledVersion.setTenantId(currentPolicy.getTenantId());
        cancelledVersion.setCreatedBy("cancellation");
        cancelledVersion.setUpdatedBy("cancellation");

        policyRepository.save(cancelledVersion);
    }

    private Map<String, Object> mergeCoverageDetails(Map<String, Object> existing, Map<String, Object> changes) {
        Map<String, Object> merged = new HashMap<>(existing);
        merged.putAll(changes);
        return merged;
    }
}
