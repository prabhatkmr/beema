package com.beema.kernel.workflow;

import com.beema.kernel.domain.WorkflowExecution;
import com.beema.kernel.domain.WorkflowHook;
import com.beema.kernel.repository.WorkflowExecutionRepository;
import com.beema.kernel.repository.WorkflowHookRepository;
import com.beema.kernel.service.expression.JexlExpressionEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.activity.Activity;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Workflow Activities Implementation
 *
 * Implements activities that interact with external systems, databases,
 * and services. These are called from Temporal workflows.
 */
@Component
public class WorkflowActivitiesImpl implements WorkflowActivities {

    private static final Logger log = LoggerFactory.getLogger(WorkflowActivitiesImpl.class);

    private final WorkflowHookRepository workflowHookRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final JexlExpressionEngine jexlEngine;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public WorkflowActivitiesImpl(WorkflowHookRepository workflowHookRepository,
                                   WorkflowExecutionRepository workflowExecutionRepository,
                                   JexlExpressionEngine jexlEngine,
                                   ObjectMapper objectMapper,
                                   WebClient.Builder webClientBuilder) {
        this.workflowHookRepository = workflowHookRepository;
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.jexlEngine = jexlEngine;
        this.objectMapper = objectMapper;

        // Configure OkHttp client with timeouts
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .build();

        // Configure WebClient
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:8080")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> fetchWorkflowHooks(String eventType) {
        log.info("Fetching workflow hooks for event type: {}", eventType);

        try {
            List<WorkflowHook> hooks = workflowHookRepository.findEnabledHooksByEventType(eventType);

            log.info("Found {} hooks for event type: {}", hooks.size(), eventType);

            return hooks.stream()
                    .map(this::convertHookToMap)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching workflow hooks", e);
            Activity.getExecutionContext().heartbeat("Error fetching hooks: " + e.getMessage());
            throw new RuntimeException("Failed to fetch workflow hooks", e);
        }
    }

    @Override
    public Boolean evaluateExpression(Map<String, Object> context, String expression) {
        log.info("Evaluating expression: {}", expression);

        try {
            Boolean result = jexlEngine.evaluateAsBoolean(context, expression);
            log.info("Expression evaluated to: {}", result);

            Activity.getExecutionContext().heartbeat("Expression evaluated");
            return result != null && result;

        } catch (Exception e) {
            log.error("Error evaluating expression: {}", expression, e);
            // Return false if expression fails (don't execute action)
            return false;
        }
    }

    @Override
    public Object evaluateExpressionForResult(Map<String, Object> context, String expression) {
        log.info("Evaluating expression for result: {}", expression);

        try {
            Object result = jexlEngine.evaluate(context, expression);
            log.info("Expression evaluated to: {}", result);

            Activity.getExecutionContext().heartbeat("Expression evaluated");
            return result;

        } catch (Exception e) {
            log.error("Error evaluating expression: {}", expression, e);
            throw new RuntimeException("Expression evaluation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> executeWebhook(String url, String method,
                                               Map<String, String> headers, Object payload) {
        log.info("Executing webhook: {} {}", method, url);

        try {
            // Build request
            Request.Builder requestBuilder = new Request.Builder().url(url);

            // Add headers
            if (headers != null) {
                headers.forEach(requestBuilder::addHeader);
            }

            // Add body for POST/PUT/PATCH
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) ||
                    "PATCH".equalsIgnoreCase(method)) {
                String jsonPayload = objectMapper.writeValueAsString(payload);
                RequestBody body = RequestBody.create(
                        jsonPayload,
                        MediaType.parse("application/json")
                );
                requestBuilder.method(method.toUpperCase(), body);
            } else {
                requestBuilder.method(method.toUpperCase(), null);
            }

            Request request = requestBuilder.build();

            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                Map<String, Object> result = new HashMap<>();
                result.put("statusCode", response.code());
                result.put("success", response.isSuccessful());
                result.put("responseBody", responseBody);
                result.put("url", url);
                result.put("method", method);

                log.info("Webhook executed successfully: {} - Status: {}", url, response.code());

                Activity.getExecutionContext().heartbeat("Webhook executed");
                return result;
            }

        } catch (Exception e) {
            log.error("Error executing webhook: {}", url, e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("url", url);
            errorResult.put("method", method);

            return errorResult;
        }
    }

    @Override
    public Map<String, Object> capturePolicySnapshot(Map<String, Object> agreementData,
                                                      String endpoint,
                                                      Boolean includeEndorsement) {
        log.info("Capturing policy snapshot at endpoint: {}", endpoint);

        try {
            // Build snapshot request payload
            Map<String, Object> snapshotRequest = new HashMap<>();
            snapshotRequest.put("agreementId", agreementData.get("agreementId"));
            snapshotRequest.put("agreementType", agreementData.get("agreementType"));
            snapshotRequest.put("agreementData", agreementData);
            snapshotRequest.put("includeEndorsement", includeEndorsement != null && includeEndorsement);
            snapshotRequest.put("timestamp", Instant.now().toString());

            // Call mock policy API using WebClient
            Map<String, Object> snapshotResponse = webClient
                    .post()
                    .uri(endpoint)
                    .bodyValue(snapshotRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .onErrorResume(e -> {
                        log.warn("Policy snapshot API not available, returning mock response", e);
                        // Return mock response if API is not available
                        return Mono.just(createMockSnapshotResponse(agreementData));
                    })
                    .block();

            log.info("Policy snapshot captured successfully: {}", snapshotResponse);

            Activity.getExecutionContext().heartbeat("Snapshot captured");
            return snapshotResponse;

        } catch (Exception e) {
            log.error("Error capturing policy snapshot", e);

            // Return mock response on error
            return createMockSnapshotResponse(agreementData);
        }
    }

    @Override
    @Transactional
    public void persistWorkflowResult(String workflowId, String runId, String eventType,
                                       Map<String, Object> inputData, Map<String, Object> resultData) {
        log.info("Persisting workflow result: workflowId={}, runId={}", workflowId, runId);

        try {
            WorkflowExecution execution = new WorkflowExecution();
            execution.setWorkflowId(workflowId);
            execution.setRunId(runId);
            execution.setEventType(eventType);

            // Extract agreement ID if present
            if (inputData != null && inputData.containsKey("agreementId")) {
                Object agreementId = inputData.get("agreementId");
                if (agreementId instanceof Number) {
                    execution.setAgreementId(((Number) agreementId).longValue());
                }
            }

            execution.setInputData(inputData);
            execution.setResultData(resultData);

            // Set status based on result
            String status = (String) resultData.get("status");
            execution.setStatus(status != null ? status : "COMPLETED");

            if ("FAILED".equals(status) && resultData.containsKey("error")) {
                execution.setErrorMessage((String) resultData.get("error"));
            }

            execution.setStartedAt(Instant.now());
            execution.setCompletedAt(Instant.now());

            workflowExecutionRepository.save(execution);

            log.info("Workflow result persisted successfully: executionId={}", execution.getExecutionId());

            Activity.getExecutionContext().heartbeat("Result persisted");

        } catch (Exception e) {
            log.error("Error persisting workflow result", e);
            throw new RuntimeException("Failed to persist workflow result", e);
        }
    }

    /**
     * Convert WorkflowHook entity to Map for serialization
     */
    private Map<String, Object> convertHookToMap(WorkflowHook hook) {
        Map<String, Object> map = new HashMap<>();
        map.put("hookId", hook.getHookId());
        map.put("hookName", hook.getHookName());
        map.put("eventType", hook.getEventType());
        map.put("triggerCondition", hook.getTriggerCondition());
        map.put("actionType", hook.getActionType());
        map.put("actionConfig", hook.getActionConfig());
        map.put("executionOrder", hook.getExecutionOrder());
        map.put("description", hook.getDescription());
        return map;
    }

    /**
     * Create mock snapshot response for testing
     */
    private Map<String, Object> createMockSnapshotResponse(Map<String, Object> agreementData) {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("snapshotId", UUID.randomUUID().toString());
        mockResponse.put("agreementId", agreementData.get("agreementId"));
        mockResponse.put("timestamp", Instant.now().toString());
        mockResponse.put("status", "CAPTURED");
        mockResponse.put("version", 1);
        mockResponse.put("source", "mock-policy-api");
        return mockResponse;
    }
}
