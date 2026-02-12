package com.beema.kernel.service.message;

import com.beema.kernel.domain.message.*;
import com.beema.kernel.service.expression.JexlExpressionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for MessageProcessingService
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = OAuth2ResourceServerAutoConfiguration.class)
@Transactional
@DisplayName("MessageProcessingService Integration Tests")
class MessageProcessingServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("beema_test")
            .withUsername("beema")
            .withPassword("beema");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MessageProcessingService processingService;

    @Autowired
    private MessageHookRepository hookRepository;

    @Autowired
    private MessageProcessingExecutionRepository executionRepository;

    @Autowired
    private JexlExpressionEngine jexlEngine;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        executionRepository.deleteAll();
    }

    @Test
    @DisplayName("Should execute complete pipeline with all stages")
    void shouldExecuteCompletePipeline() {
        // Given
        MessageHook hook = createTestHook();
        hook = hookRepository.save(hook);

        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", " ABC123 ");
        message.put("customerName", "John Doe");
        message.put("premiumAmount", 1000);

        MessageProcessingContext context = new MessageProcessingContext(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .hookId(hook.getHookId())
                .hookName(hook.getHookName());

        // When
        context = processingService.executeFullPipeline(context, List.of(hook));

        // Then
        assertThat(context.isHasErrors()).isFalse();
        assertThat(context.getResult()).isNotNull();
        assertThat(context.getResult().get("agreementId")).isEqualTo("ABC123"); // Trimmed and uppercased
        assertThat(context.getStageResults()).containsKeys("preprocessing", "transformation", "postprocessing");
    }

    @Test
    @DisplayName("Should handle preprocessing validation failure with fail_fast")
    void shouldHandlePreprocessingValidationFailure() {
        // Given
        MessageHook hook = new MessageHook();
        hook.setHookName("validation_hook");
        hook.setMessageType("policy.created");
        hook.setSourceSystem("retail_system");
        hook.setPreprocessingJexl("if (message.policyNumber == null) { throw 'Missing policy number'; }");
        hook.setTransformationJexl("message");
        hook.setErrorHandlingStrategy("fail_fast");
        hook.setEnabled(true);
        hook = hookRepository.save(hook);

        Map<String, Object> message = new HashMap<>();
        message.put("customerName", "John Doe");
        // Missing policyNumber

        MessageProcessingContext context = new MessageProcessingContext(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .hookId(hook.getHookId())
                .hookName(hook.getHookName());

        // When
        context = processingService.executeFullPipeline(context, List.of(hook));

        // Then
        assertThat(context.isHasErrors()).isTrue();
        assertThat(context.getErrorMessage()).contains("Missing policy number");
    }

    @Test
    @DisplayName("Should retry on failure with retry strategy")
    void shouldRetryOnFailure() {
        // Given
        MessageHook hook = new MessageHook();
        hook.setHookName("retry_hook");
        hook.setMessageType("policy.created");
        hook.setSourceSystem("retail_system");
        hook.setTransformationJexl("if (message.failCount < 2) { message.failCount = (message.failCount != null ? message.failCount : 0) + 1; throw 'Temporary failure'; } else { message }");
        hook.setErrorHandlingStrategy("retry");

        Map<String, Object> retryConfig = new HashMap<>();
        retryConfig.put("maxAttempts", 3);
        retryConfig.put("backoffMs", 100);
        retryConfig.put("backoffMultiplier", 1.5);
        hook.setRetryConfig(retryConfig);
        hook.setEnabled(true);
        hook = hookRepository.save(hook);

        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "TEST123");
        message.put("failCount", 0);

        MessageProcessingContext context = new MessageProcessingContext(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .hookId(hook.getHookId())
                .hookName(hook.getHookName());

        // When
        context = processingService.executeTransformation(context, hook);

        // Then
        assertThat(context.isHasErrors()).isFalse();
        assertThat(context.getAttemptNumber()).isGreaterThan(1);
    }

    @Test
    @DisplayName("Should log and continue with log_continue strategy")
    void shouldLogAndContinue() {
        // Given
        MessageHook hook = new MessageHook();
        hook.setHookName("log_continue_hook");
        hook.setMessageType("policy.created");
        hook.setSourceSystem("retail_system");
        hook.setTransformationJexl("throw 'Expected error'");
        hook.setErrorHandlingStrategy("log_continue");
        hook.setEnabled(true);
        hook = hookRepository.save(hook);

        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "TEST123");

        MessageProcessingContext context = new MessageProcessingContext(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .hookId(hook.getHookId())
                .hookName(hook.getHookName());

        // When
        context = processingService.executeTransformation(context, hook);

        // Then
        // Error should be logged but processing continues (error cleared)
        assertThat(context.isHasErrors()).isFalse();
    }

    @Test
    @DisplayName("Should record execution history")
    void shouldRecordExecutionHistory() {
        // Given
        MessageHook hook = createTestHook();
        hook = hookRepository.save(hook);

        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "TEST123");
        message.put("customerName", "John Doe");
        message.put("premiumAmount", 1000);

        MessageProcessingContext context = new MessageProcessingContext(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .hookId(hook.getHookId())
                .hookName(hook.getHookName());

        // When
        context = processingService.executeFullPipeline(context, List.of(hook));

        // Then
        List<MessageProcessingExecution> executions = executionRepository.findByHookIdOrderByStartedAtDesc(hook.getHookId());
        assertThat(executions).isNotEmpty();
        assertThat(executions).hasSize(3); // preprocessing, transformation, postprocessing

        MessageProcessingExecution transformationExecution = executions.stream()
                .filter(e -> "transformation".equals(e.getProcessingStage()))
                .findFirst()
                .orElseThrow();

        assertThat(transformationExecution.getStatus()).isEqualTo("SUCCESS");
        assertThat(transformationExecution.getExecutionTimeMs()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should execute postprocessing with calculated fields")
    void shouldExecutePostprocessingWithCalculatedFields() {
        // Given
        MessageHook hook = new MessageHook();
        hook.setHookName("postprocess_hook");
        hook.setMessageType("policy.created");
        hook.setSourceSystem("retail_system");
        hook.setTransformationJexl("{ 'agreementId': message.policyNumber, 'premium': { 'amount': message.premiumAmount, 'frequency': 'MONTHLY' } }");
        hook.setPostprocessingJexl(
                "if (result.premium.frequency == 'MONTHLY') { " +
                        "result.premium.annualAmount = result.premium.amount * 12; " +
                        "}"
        );
        hook.setErrorHandlingStrategy("fail_fast");
        hook.setEnabled(true);
        hook = hookRepository.save(hook);

        Map<String, Object> message = new HashMap<>();
        message.put("policyNumber", "TEST123");
        message.put("premiumAmount", 100);

        MessageProcessingContext context = new MessageProcessingContext(message)
                .messageType("policy.created")
                .sourceSystem("retail_system")
                .hookId(hook.getHookId())
                .hookName(hook.getHookName());

        // When
        context = processingService.executeFullPipeline(context, List.of(hook));

        // Then
        assertThat(context.isHasErrors()).isFalse();

        @SuppressWarnings("unchecked")
        Map<String, Object> premium = (Map<String, Object>) context.getResult().get("premium");
        assertThat(premium).isNotNull();
        assertThat(premium.get("annualAmount")).isEqualTo(1200);
    }

    // Helper methods

    private MessageHook createTestHook() {
        MessageHook hook = new MessageHook();
        hook.setHookName("test_hook_" + UUID.randomUUID());
        hook.setMessageType("policy.created");
        hook.setSourceSystem("retail_system");

        // Pre-processing: normalize
        hook.setPreprocessingJexl(
                "message.policyNumber = message.policyNumber.trim().toUpperCase(); " +
                        "message.validated = true;"
        );
        hook.setPreprocessingOrder(10);

        // Transformation: map fields
        hook.setTransformationJexl(
                "{ " +
                        "'agreementId': message.policyNumber, " +
                        "'customer': { 'name': message.customerName }, " +
                        "'premium': { 'amount': message.premiumAmount } " +
                        "}"
        );
        hook.setTransformationOrder(100);

        // Post-processing: add metadata
        hook.setPostprocessingJexl(
                "result.processedAt = new('java.time.Instant').now().toString(); " +
                        "result.validated = message.validated;"
        );
        hook.setPostprocessingOrder(200);

        hook.setErrorHandlingStrategy("fail_fast");
        hook.setEnabled(true);

        return hook;
    }
}
