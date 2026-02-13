package com.beema.kernel.workflow.submission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Mock implementation of RatingActivities.
 *
 * Returns fixed premium values for development and testing.
 * Replace with actual rating engine integration in production.
 */
public class RatingActivitiesImpl implements RatingActivities {

    private static final Logger log = LoggerFactory.getLogger(RatingActivitiesImpl.class);

    @Override
    public Map<String, Object> rate(Map<String, Object> data) {
        log.info("Rating submission with data keys: {}", data != null ? data.keySet() : "null");

        // Mock rating calculation
        double premium = 500.0;
        double tax = 50.0;
        double total = premium + tax;

        log.info("Rating complete: premium={}, tax={}, total={}", premium, tax, total);

        return Map.of(
                "premium", premium,
                "tax", tax,
                "total", total
        );
    }
}
