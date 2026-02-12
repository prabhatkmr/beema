package com.beema.metadata.service;

import com.beema.metadata.dto.MessageHookMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listens to PostgreSQL NOTIFY events for message hook changes
 * and publishes MessageHookMetadata to Kafka control stream.
 */
@Service
public class MessageHookEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MessageHookEventPublisher.class);
    private static final String NOTIFICATION_CHANNEL = "message_hook_changed";

    private final DataSource dataSource;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String controlTopicName;

    private ScheduledExecutorService executorService;
    private volatile boolean running = false;

    public MessageHookEventPublisher(
            DataSource dataSource,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${beema.kafka.control-topic:message-hooks-control}") String controlTopicName) {
        this.dataSource = dataSource;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.controlTopicName = controlTopicName;
    }

    @PostConstruct
    public void startListening() {
        this.running = true;
        this.executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "PostgreSQL-NOTIFY-Listener");
            thread.setDaemon(true);
            return thread;
        });

        executorService.submit(this::listenToNotifications);
        log.info("Started PostgreSQL NOTIFY listener for channel: {}", NOTIFICATION_CHANNEL);
    }

    @PreDestroy
    public void stopListening() {
        this.running = false;
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("Stopped PostgreSQL NOTIFY listener");
    }

    private void listenToNotifications() {
        while (running) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                PGConnection pgConn = conn.unwrap(PGConnection.class);

                // Listen to the notification channel
                stmt.execute("LISTEN " + NOTIFICATION_CHANNEL);
                log.debug("LISTEN command executed for channel: {}", NOTIFICATION_CHANNEL);

                while (running && !conn.isClosed()) {
                    // Check for notifications with a timeout
                    PGNotification[] notifications = pgConn.getNotifications(5000);

                    if (notifications != null && notifications.length > 0) {
                        for (PGNotification notification : notifications) {
                            processNotification(notification);
                        }
                    }

                    // Sleep briefly to avoid tight loop
                    Thread.sleep(100);
                }

            } catch (InterruptedException e) {
                log.warn("PostgreSQL NOTIFY listener interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in PostgreSQL NOTIFY listener: {}", e.getMessage(), e);
                // Retry after a delay
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("PostgreSQL NOTIFY listener stopped");
    }

    private void processNotification(PGNotification notification) {
        try {
            String payload = notification.getParameter();
            log.debug("Received notification: channel={}, payload={}", notification.getName(), payload);

            // Parse the JSON payload from PostgreSQL trigger
            JsonNode jsonNode = objectMapper.readTree(payload);

            MessageHookMetadata metadata = new MessageHookMetadata();
            metadata.setHookId(jsonNode.get("hookId").asText());
            metadata.setMessageType(jsonNode.get("messageType").asText());
            metadata.setScript(jsonNode.has("script") ? jsonNode.get("script").asText() : null);
            metadata.setEnabled(jsonNode.get("enabled").asBoolean());
            metadata.setOperation(jsonNode.get("operation").asText());
            metadata.setUpdatedAt(Instant.now());

            // Publish to Kafka control stream
            publishToKafka(metadata);

        } catch (Exception e) {
            log.error("Failed to process notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Publishes MessageHookMetadata to Kafka control stream.
     *
     * @param metadata MessageHookMetadata to publish
     */
    public void publishToKafka(MessageHookMetadata metadata) {
        try {
            // Use messageType as the key for partitioning
            String key = metadata.getMessageType();

            kafkaTemplate.send(controlTopicName, key, metadata)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Published MessageHookMetadata to Kafka: topic={}, key={}, hookId={}, operation={}",
                                    controlTopicName, key, metadata.getHookId(), metadata.getOperation());
                        } else {
                            log.error("Failed to publish MessageHookMetadata to Kafka: {}", ex.getMessage(), ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing to Kafka: {}", e.getMessage(), e);
        }
    }
}
