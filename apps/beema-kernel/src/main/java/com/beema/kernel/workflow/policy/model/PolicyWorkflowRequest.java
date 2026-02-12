package com.beema.kernel.workflow.policy.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Policy Workflow Request
 *
 * Input data for initiating a policy lifecycle workflow.
 */
public class PolicyWorkflowRequest {

    @NotBlank(message = "Policy ID is required")
    private String policyId;

    @NotBlank(message = "Initial state is required")
    private String initialState;

    @NotNull(message = "Metadata cannot be null")
    private Map<String, Object> metadata;

    public PolicyWorkflowRequest() {
        this.metadata = new HashMap<>();
    }

    public PolicyWorkflowRequest(String policyId, String initialState, Map<String, Object> metadata) {
        this.policyId = policyId;
        this.initialState = initialState;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getInitialState() {
        return initialState;
    }

    public void setInitialState(String initialState) {
        this.initialState = initialState;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "PolicyWorkflowRequest{" +
                "policyId='" + policyId + '\'' +
                ", initialState='" + initialState + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}
