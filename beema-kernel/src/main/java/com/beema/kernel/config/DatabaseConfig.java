package com.beema.kernel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Database and JSON configuration.
 *
 * Configures:
 * - ObjectMapper for JSONB serialization
 * - Java 8 Time support (OffsetDateTime, LocalDateTime)
 */
@Configuration
public class DatabaseConfig {

    /**
     * Configure ObjectMapper for JSONB conversion.
     *
     * Features:
     * - Java 8 Time API support (OffsetDateTime, LocalDateTime)
     * - ISO-8601 date formatting
     * - Pretty printing disabled (compact JSON in database)
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Support Java 8 Time API
        mapper.registerModule(new JavaTimeModule());

        // Use ISO-8601 for dates instead of timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Compact JSON (no pretty printing in database)
        mapper.disable(SerializationFeature.INDENT_OUTPUT);

        // Don't fail on empty beans
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return mapper;
    }
}
