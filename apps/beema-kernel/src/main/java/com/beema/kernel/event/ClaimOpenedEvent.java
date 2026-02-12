package com.beema.kernel.event;

/**
 * Event published when a new claim is opened
 */
public class ClaimOpenedEvent extends DomainEvent {

    public ClaimOpenedEvent() {
        super("claim/opened");
    }

    public ClaimOpenedEvent(String claimNumber, String claimId, Double claimAmount, String claimType) {
        super("claim/opened");
        withData("claimNumber", claimNumber);
        withData("claimId", claimId);
        withData("claimAmount", claimAmount);
        withData("claimType", claimType);
    }
}
