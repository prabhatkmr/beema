package com.beema.kernel.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Publishes domain events to Inngest for webhook fan-out and processing
 */
@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${inngest.event-key}")
    private String inngestEventKey;

    @Value("${inngest.base-url}")
    private String inngestBaseUrl;

    @Value("${beema.events.publisher.enabled}")
    private boolean publisherEnabled;

    @Value("${beema.events.publisher.async}")
    private boolean asyncPublishing;

    public DomainEventPublisher(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Publish a domain event to Inngest
     */
    @Async("eventPublisherExecutor")
    public void publish(DomainEvent event) {
        if (!publisherEnabled) {
            log.debug("Event publishing disabled, skipping event: {}", event.getEventName());
            return;
        }

        log.info("Publishing event: {} (ID: {})", event.getEventName(), event.getEventId());

        try {
            // Prepare Inngest event payload
            Map<String, Object> inngestEvent = Map.of(
                "name", event.getEventName(),
                "data", event.getData(),
                "user", event.getUser(),
                "ts", event.getTimestamp(),
                "v", event.getVersion()
            );

            // Send to Inngest
            webClient.post()
                .uri(inngestBaseUrl + "/e/" + inngestEventKey)
                .header("Content-Type", "application/json")
                .bodyValue(inngestEvent)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response ->
                    log.info("Event published successfully: {} - Response: {}", event.getEventName(), response))
                .doOnError(error ->
                    log.error("Failed to publish event: {}", event.getEventName(), error))
                .onErrorResume(error -> {
                    // Don't fail the main operation if event publishing fails
                    log.warn("Event publishing failed, continuing: {}", error.getMessage());
                    return Mono.empty();
                })
                .subscribe();

        } catch (Exception e) {
            log.error("Error publishing event: {}", event.getEventName(), e);
        }
    }

    /**
     * Publish multiple events in batch
     */
    @Async("eventPublisherExecutor")
    public void publishBatch(DomainEvent... events) {
        for (DomainEvent event : events) {
            publish(event);
        }
    }

    /**
     * Publish with custom metadata
     */
    @Async("eventPublisherExecutor")
    public void publishWithMetadata(DomainEvent event, String tenantId, String userId, String email) {
        event.withUser(userId, email);
        event.withData("tenantId", tenantId);
        publish(event);
    }
}
