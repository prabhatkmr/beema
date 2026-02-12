package com.beema.kernel.event;

import java.time.OffsetDateTime;

/**
 * Event published when a policy is renewed
 */
public class PolicyRenewedEvent extends DomainEvent {

    public PolicyRenewedEvent() {
        super("policy/renewed");
    }

    public PolicyRenewedEvent(String policyNumber, String newPolicyNumber, OffsetDateTime renewalDate) {
        super("policy/renewed");
        withData("policyNumber", policyNumber);
        withData("newPolicyNumber", newPolicyNumber);
        withData("renewalDate", renewalDate.toString());
    }
}
