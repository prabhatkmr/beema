package com.beema.kernel.workflow.policy;

public record ProRataResult(
        Double adjustment,
        Integer daysRemaining,
        Double dailyRate
) {
}
