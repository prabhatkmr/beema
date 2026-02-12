package com.beema.kernel.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;

/**
 * Agreement Workflow Implementation
 *
 * Temporal workflow that acts as a DSL interpreter:
 * 1. Fetches workflow hooks from database for the given event type
 * 2. Evaluates trigger conditions using JEXL
 * 3. Executes actions (webhooks, snapshots, expressions) based on configuration
 * 4. Persists workflow results to database
 */
public class AgreementWorkflowImpl implements AgreementWorkflow {

    private static final Logger log = Workflow.getLogger(AgreementWorkflowImpl.class);

    // Configure activity options with retries and timeouts
    private final ActivityOptions activityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            .setRetryOptions(io.temporal.common.RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setBackoffCoefficient(2.0)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .build())
            .build();

    private final WorkflowActivities activities = Workflow.newActivityStub(
            WorkflowActivities.class,
            activityOptions
    );

    @Override
    public Map<String, Object> executeAgreementWorkflow(String eventType, Map<String, Object> agreementData) {
        log.info("Starting Agreement Workflow for event: {}", eventType);

        Map<String, Object> workflowResult = new HashMap<>();
        workflowResult.put("eventType", eventType);
        workflowResult.put("workflowId", Workflow.getInfo().getWorkflowId());
        workflowResult.put("runId", Workflow.getInfo().getRunId());
        workflowResult.put("startTime", System.currentTimeMillis());

        List<Map<String, Object>> actionResults = new ArrayList<>();

        try {
            // Step 1: Fetch workflow hooks from database
            log.info("Fetching workflow hooks for event type: {}", eventType);
            List<Map<String, Object>> hooks = activities.fetchWorkflowHooks(eventType);

            if (hooks == null || hooks.isEmpty()) {
                log.info("No workflow hooks found for event type: {}", eventType);
                workflowResult.put("status", "NO_HOOKS");
                workflowResult.put("message", "No workflow hooks configured for this event");
                return workflowResult;
            }

            log.info("Found {} workflow hooks for event type: {}", hooks.size(), eventType);

            // Step 2: Process each hook in execution order
            for (Map<String, Object> hook : hooks) {
                String hookName = (String) hook.get("hookName");
                String triggerCondition = (String) hook.get("triggerCondition");
                String actionType = (String) hook.get("actionType");
                @SuppressWarnings("unchecked")
                Map<String, Object> actionConfig = (Map<String, Object>) hook.get("actionConfig");

                log.info("Processing hook: {} (type: {})", hookName, actionType);

                Map<String, Object> actionResult = new HashMap<>();
                actionResult.put("hookName", hookName);
                actionResult.put("actionType", actionType);

                try {
                    // Step 3: Evaluate trigger condition using JEXL
                    log.info("Evaluating trigger condition: {}", triggerCondition);
                    Boolean shouldExecute = activities.evaluateExpression(agreementData, triggerCondition);

                    actionResult.put("conditionResult", shouldExecute);

                    if (Boolean.TRUE.equals(shouldExecute)) {
                        log.info("Trigger condition passed, executing action: {}", actionType);

                        // Step 4: Execute action based on type
                        Object executionResult = executeAction(actionType, actionConfig, agreementData);
                        actionResult.put("status", "SUCCESS");
                        actionResult.put("result", executionResult);

                        log.info("Action executed successfully: {}", hookName);
                    } else {
                        log.info("Trigger condition not met, skipping action: {}", hookName);
                        actionResult.put("status", "SKIPPED");
                        actionResult.put("reason", "Trigger condition evaluated to false");
                    }

                } catch (Exception e) {
                    log.error("Error executing hook: {}", hookName, e);
                    actionResult.put("status", "FAILED");
                    actionResult.put("error", e.getMessage());
                }

                actionResults.add(actionResult);
            }

            workflowResult.put("status", "COMPLETED");
            workflowResult.put("actionResults", actionResults);
            workflowResult.put("totalHooks", hooks.size());
            workflowResult.put("successfulActions",
                    actionResults.stream().filter(r -> "SUCCESS".equals(r.get("status"))).count());

        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            workflowResult.put("status", "FAILED");
            workflowResult.put("error", e.getMessage());
            workflowResult.put("actionResults", actionResults);
        } finally {
            workflowResult.put("endTime", System.currentTimeMillis());

            // Step 5: Persist workflow result to database
            try {
                activities.persistWorkflowResult(
                        Workflow.getInfo().getWorkflowId(),
                        Workflow.getInfo().getRunId(),
                        eventType,
                        agreementData,
                        workflowResult
                );
                log.info("Workflow result persisted successfully");
            } catch (Exception e) {
                log.error("Failed to persist workflow result", e);
                // Don't fail the workflow if persistence fails
            }
        }

        log.info("Agreement Workflow completed with status: {}", workflowResult.get("status"));
        return workflowResult;
    }

    /**
     * Execute action based on action type
     */
    private Object executeAction(String actionType, Map<String, Object> actionConfig,
                                  Map<String, Object> agreementData) {
        return switch (actionType.toLowerCase()) {
            case "webhook" -> executeWebhookAction(actionConfig, agreementData);
            case "snapshot" -> executeSnapshotAction(actionConfig, agreementData);
            case "expression" -> executeExpressionAction(actionConfig, agreementData);
            case "custom_logic" -> executeCustomLogicAction(actionConfig, agreementData);
            default -> {
                log.warn("Unknown action type: {}", actionType);
                yield Map.of("error", "Unknown action type: " + actionType);
            }
        };
    }

    /**
     * Execute webhook action - calls external HTTP endpoint
     */
    private Object executeWebhookAction(Map<String, Object> config, Map<String, Object> agreementData) {
        String url = (String) config.get("url");
        String method = (String) config.getOrDefault("method", "POST");
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) config.getOrDefault("headers", new HashMap<>());
        Object payload = config.getOrDefault("payload", agreementData);

        log.info("Executing webhook: {} {}", method, url);

        return activities.executeWebhook(url, method, headers, payload);
    }

    /**
     * Execute snapshot action - captures policy snapshot
     */
    private Object executeSnapshotAction(Map<String, Object> config, Map<String, Object> agreementData) {
        String endpoint = (String) config.getOrDefault("endpoint", "/mock-policy-api/snapshots");
        Boolean includeEndorsement = (Boolean) config.getOrDefault("includeEndorsement", false);

        log.info("Capturing policy snapshot at endpoint: {}", endpoint);

        return activities.capturePolicySnapshot(agreementData, endpoint, includeEndorsement);
    }

    /**
     * Execute expression action - evaluates JEXL expression and stores result
     */
    private Object executeExpressionAction(Map<String, Object> config, Map<String, Object> agreementData) {
        String expression = (String) config.get("expression");
        String resultField = (String) config.get("resultField");

        log.info("Evaluating expression: {}", expression);

        Object result = activities.evaluateExpressionForResult(agreementData, expression);

        return Map.of(
                "expression", expression,
                "resultField", resultField,
                "result", result
        );
    }

    /**
     * Execute custom logic action - placeholder for custom business logic
     */
    private Object executeCustomLogicAction(Map<String, Object> config, Map<String, Object> agreementData) {
        String logicType = (String) config.get("logicType");

        log.info("Executing custom logic: {}", logicType);

        // Placeholder for custom business logic
        return Map.of(
                "logicType", logicType,
                "status", "EXECUTED",
                "message", "Custom logic executed successfully"
        );
    }
}
