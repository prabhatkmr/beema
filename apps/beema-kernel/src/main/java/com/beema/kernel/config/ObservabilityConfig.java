package com.beema.kernel.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Configuration
public class ObservabilityConfig {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public OncePerRequestFilter correlationIdFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String correlationId = request.getHeader(CORRELATION_ID_HEADER);
                if (correlationId == null || correlationId.isBlank()) {
                    correlationId = UUID.randomUUID().toString();
                }
                MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
                response.setHeader(CORRELATION_ID_HEADER, correlationId);
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    MDC.remove(CORRELATION_ID_MDC_KEY);
                }
            }
        };
    }
}
