package com.beema.kernel.config.metrics;

import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.metadata.MarketContext;
import com.beema.kernel.service.agreement.AgreementService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Custom metrics for agreements.
 *
 * Exposes:
 * - Agreement count by market context
 * - Agreement count by status
 * - Cache hit rates
 */
@Component
public class AgreementMetrics {

    private final AgreementService agreementService;
    private final MeterRegistry meterRegistry;

    private static final String TENANT_ID = "default"; // TODO: Get from context

    public AgreementMetrics(
        AgreementService agreementService,
        MeterRegistry meterRegistry
    ) {
        this.agreementService = agreementService;
        this.meterRegistry = meterRegistry;

        registerMetrics();
    }

    private void registerMetrics() {
        // Register gauges for each market context
        for (MarketContext context : MarketContext.values()) {
            Gauge.builder("beema.agreements.count", this,
                    value -> agreementService.countByTenantAndContext(TENANT_ID, context))
                .tag("market_context", context.name())
                .tag("tenant", TENANT_ID)
                .description("Number of current agreements by market context")
                .register(meterRegistry);
        }

        // Register gauges for each status
        for (AgreementStatus status : AgreementStatus.values()) {
            Gauge.builder("beema.agreements.by_status", this,
                    value -> agreementService.findAgreementsByStatus(TENANT_ID, status).size())
                .tag("status", status.name())
                .tag("tenant", TENANT_ID)
                .description("Number of agreements by status")
                .register(meterRegistry);
        }
    }

    /**
     * Update metrics periodically.
     * Gauges are lazily evaluated, but we can trigger collection.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void updateMetrics() {
        // Metrics are automatically collected when scraped
        // This method exists for potential future custom metrics
    }
}
