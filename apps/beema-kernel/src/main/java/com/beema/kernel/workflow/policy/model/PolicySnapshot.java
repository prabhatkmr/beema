package com.beema.kernel.workflow.policy.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Policy Snapshot
 *
 * Represents a point-in-time snapshot of a policy's data.
 */
public class PolicySnapshot {

    private String snapshotId;
    private String policyId;
    private String policyNumber;
    private String state;
    private Instant capturedAt;
    private Map<String, Object> policyData;
    private String version;

    public PolicySnapshot() {
        this.snapshotId = UUID.randomUUID().toString();
        this.capturedAt = Instant.now();
        this.policyData = new HashMap<>();
    }

    public PolicySnapshot(String policyId, String policyNumber, String state, Map<String, Object> policyData) {
        this.snapshotId = UUID.randomUUID().toString();
        this.policyId = policyId;
        this.policyNumber = policyNumber;
        this.state = state;
        this.capturedAt = Instant.now();
        this.policyData = policyData != null ? policyData : new HashMap<>();
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public Map<String, Object> getPolicyData() {
        return policyData;
    }

    public void setPolicyData(Map<String, Object> policyData) {
        this.policyData = policyData;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "PolicySnapshot{" +
                "snapshotId='" + snapshotId + '\'' +
                ", policyId='" + policyId + '\'' +
                ", policyNumber='" + policyNumber + '\'' +
                ", state='" + state + '\'' +
                ", capturedAt=" + capturedAt +
                ", version='" + version + '\'' +
                '}';
    }
}
