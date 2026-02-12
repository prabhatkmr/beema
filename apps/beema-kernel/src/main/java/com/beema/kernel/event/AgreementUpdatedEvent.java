package com.beema.kernel.event;

import java.util.Map;

/**
 * Event published when an agreement is updated
 */
public class AgreementUpdatedEvent extends DomainEvent {

    public AgreementUpdatedEvent() {
        super("agreement/updated");
    }

    public AgreementUpdatedEvent(String agreementId, String changeType, Map<String, Object> changes) {
        super("agreement/updated");
        withData("agreementId", agreementId);
        withData("changeType", changeType);
        withData("changes", changes);
    }
}
