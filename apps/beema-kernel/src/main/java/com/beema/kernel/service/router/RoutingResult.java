package com.beema.kernel.service.router;

import com.beema.kernel.domain.user.SysUser;

/**
 * Result of task routing operation.
 */
public record RoutingResult(
        SysUser assignedUser,
        double matchScore,
        String reasoning
) {

    public static RoutingResult noMatch() {
        return new RoutingResult(null, 0.0, "No eligible users found");
    }

    public boolean hasMatch() {
        return assignedUser != null;
    }
}
