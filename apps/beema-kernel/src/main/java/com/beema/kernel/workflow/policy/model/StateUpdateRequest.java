package com.beema.kernel.workflow.policy.model;

import jakarta.validation.constraints.NotBlank;

/**
 * State Update Request
 *
 * Request to update the state of a running policy workflow.
 */
public class StateUpdateRequest {

    @NotBlank(message = "New state is required")
    private String newState;

    private String reason;

    public StateUpdateRequest() {
    }

    public StateUpdateRequest(String newState, String reason) {
        this.newState = newState;
        this.reason = reason;
    }

    public String getNewState() {
        return newState;
    }

    public void setNewState(String newState) {
        this.newState = newState;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "StateUpdateRequest{" +
                "newState='" + newState + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}
