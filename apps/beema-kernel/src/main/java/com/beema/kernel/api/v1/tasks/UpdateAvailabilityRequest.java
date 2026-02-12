package com.beema.kernel.api.v1.tasks;

import com.beema.kernel.domain.user.AvailabilityStatus;

public record UpdateAvailabilityRequest(
        AvailabilityStatus status
) {
}
