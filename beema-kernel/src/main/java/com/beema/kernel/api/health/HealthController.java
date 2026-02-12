package com.beema.kernel.api.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Basic health check endpoint.
 *
 * Provides a simple status endpoint independent of Spring Actuator
 * for basic readiness checks.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Basic health check endpoint.
     *
     * @return Status information
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "application", "beema-kernel",
            "version", "1.0.0",
            "timestamp", OffsetDateTime.now()
        );
    }

    /**
     * Application information endpoint.
     *
     * @return Application metadata
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
            "application", Map.of(
                "name", "Beema Unified Agreement Kernel",
                "description", "Bitemporal, metadata-driven insurance agreement system",
                "version", "1.0.0",
                "architecture", "Bitemporal with JSONB flex-schema"
            ),
            "features", Map.of(
                "bitemporal", true,
                "multiTenancy", true,
                "marketContexts", new String[]{"RETAIL", "COMMERCIAL", "LONDON_MARKET"},
                "jsonbFlexSchema", true,
                "rowLevelSecurity", true
            ),
            "stack", Map.of(
                "java", System.getProperty("java.version"),
                "springBoot", "3.2.1",
                "database", "PostgreSQL 15.4"
            )
        );
    }
}
