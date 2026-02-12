package com.beema.kernel.workflow.policy.model;

import java.time.Instant;

/**
 * Policy Workflow Result
 *
 * Output data from a completed policy lifecycle workflow.
 */
public class PolicyWorkflowResult {

    private String finalState;
    private String snapshotId;
    private Instant timestamp;
    private boolean success;
    private String message;

    public PolicyWorkflowResult() {
    }

    public PolicyWorkflowResult(String finalState, String snapshotId, Instant timestamp, boolean success, String message) {
        this.finalState = finalState;
        this.snapshotId = snapshotId;
        this.timestamp = timestamp;
        this.success = success;
        this.message = message;
    }

    public static PolicyWorkflowResult success(String finalState, String snapshotId) {
        return new PolicyWorkflowResult(
                finalState,
                snapshotId,
                Instant.now(),
                true,
                "Policy workflow completed successfully"
        );
    }

    public static PolicyWorkflowResult failure(String finalState, String message) {
        return new PolicyWorkflowResult(
                finalState,
                null,
                Instant.now(),
                false,
                message
        );
    }

    public String getFinalState() {
        return finalState;
    }

    public void setFinalState(String finalState) {
        this.finalState = finalState;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "PolicyWorkflowResult{" +
                "finalState='" + finalState + '\'' +
                ", snapshotId='" + snapshotId + '\'' +
                ", timestamp=" + timestamp +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
