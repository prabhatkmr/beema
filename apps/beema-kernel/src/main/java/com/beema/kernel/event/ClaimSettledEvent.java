package com.beema.kernel.event;

/**
 * Event published when a claim is settled
 */
public class ClaimSettledEvent extends DomainEvent {

    public ClaimSettledEvent() {
        super("claim/settled");
    }

    public ClaimSettledEvent(String claimNumber, Double settlementAmount, String settlementType) {
        super("claim/settled");
        withData("claimNumber", claimNumber);
        withData("settlementAmount", settlementAmount);
        withData("settlementType", settlementType);
    }
}
