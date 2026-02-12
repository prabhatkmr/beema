package com.beema.kernel.event;

/**
 * Event published when a policy is bound/issued
 */
public class PolicyBoundEvent extends DomainEvent {

    public PolicyBoundEvent() {
        super("policy/bound");
    }

    public PolicyBoundEvent(String policyNumber, String agreementId, String marketContext) {
        super("policy/bound");
        withData("policyNumber", policyNumber);
        withData("agreementId", agreementId);
        withData("marketContext", marketContext);
    }
}
