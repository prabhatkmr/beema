package com.beema.kernel.config.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Custom health indicator for metadata cache.
 *
 * Checks:
 * - Cache manager is available
 * - Required caches exist
 * - Cache statistics (if available)
 */
@Component
public class MetadataCacheHealthIndicator implements HealthIndicator {

    private final CacheManager cacheManager;

    public MetadataCacheHealthIndicator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Health health() {
        try {
            Collection<String> cacheNames = cacheManager.getCacheNames();

            boolean hasAgreementTypesCache = cacheNames.contains("metadata-agreement-types");
            boolean hasAttributesCache = cacheNames.contains("metadata-attributes");

            if (hasAgreementTypesCache && hasAttributesCache) {
                return Health.up()
                    .withDetail("cacheManager", cacheManager.getClass().getSimpleName())
                    .withDetail("caches", cacheNames)
                    .withDetail("status", "All required caches present")
                    .build();
            } else {
                return Health.down()
                    .withDetail("caches", cacheNames)
                    .withDetail("missing", !hasAgreementTypesCache ? "metadata-agreement-types" : "metadata-attributes")
                    .build();
            }

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }
}
