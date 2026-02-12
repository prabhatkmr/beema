package com.beema.kernel.workflow.policy;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Rating activities for premium calculations.
 */
@Component
public class RatingActivitiesImpl implements RatingActivities {

    @Override
    public ProRataResult calculateProRata(
            Double oldPremium,
            Double newPremium,
            LocalDateTime effectiveDate,
            LocalDateTime expiryDate) {

        long daysRemaining = ChronoUnit.DAYS.between(effectiveDate, expiryDate);
        long totalDays = ChronoUnit.DAYS.between(effectiveDate.minusYears(1), expiryDate);

        if (totalDays == 0) {
            totalDays = 365;
        }

        double dailyOldRate = oldPremium / totalDays;
        double dailyNewRate = newPremium / totalDays;

        double refund = dailyOldRate * daysRemaining;
        double newCharge = dailyNewRate * daysRemaining;

        double adjustment = newCharge - refund;

        return new ProRataResult(
                adjustment,
                (int) daysRemaining,
                dailyNewRate
        );
    }
}
