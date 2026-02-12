package com.beema.kernel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Beema Unified Agreement Kernel
 *
 * Bitemporal, metadata-driven insurance agreement system supporting:
 * - Retail Insurance
 * - Commercial Insurance
 * - London Market
 *
 * Architecture:
 * - Bitemporal tracking (valid_time + transaction_time)
 * - JSONB flex-schema for market-specific attributes
 * - Multi-tenancy with Row-Level Security
 * - Metadata-driven validation
 *
 * @author Beema Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
public class KernelApplication {

    public static void main(String[] args) {
        SpringApplication.run(KernelApplication.class, args);
    }
}
