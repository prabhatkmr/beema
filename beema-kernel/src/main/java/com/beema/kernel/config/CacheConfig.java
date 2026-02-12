package com.beema.kernel.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine.
 *
 * Caches:
 * - metadata-agreement-types: Agreement type schemas (1 hour TTL)
 * - metadata-attributes: Attribute definitions (1 hour TTL)
 *
 * Configuration:
 * - Maximum size: 1000 entries per cache
 * - Expire after write: 3600 seconds (1 hour)
 * - Record stats: true
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "metadata-agreement-types",
            "metadata-attributes"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(3600, TimeUnit.SECONDS)
            .recordStats()
        );

        return cacheManager;
    }
}
