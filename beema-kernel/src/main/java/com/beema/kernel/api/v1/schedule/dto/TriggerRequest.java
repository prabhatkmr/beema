package com.beema.kernel.api.v1.schedule.dto;

import java.util.Map;

/**
 * Request DTO for triggering an ad-hoc execution of a schedule.
 */
public record TriggerRequest(
    Map<String, Object> overrideParams
) {
    public TriggerRequest {
        if (overrideParams == null) {
            overrideParams = Map.of();
        }
    }
}
