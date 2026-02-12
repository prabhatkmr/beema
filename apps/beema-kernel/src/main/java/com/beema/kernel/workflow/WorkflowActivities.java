package com.beema.kernel.workflow;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;
import java.util.Map;

/**
 * Workflow Activities Interface
 *
 * Defines activities that can be called from Temporal workflows.
 * Activities are the units of work that interact with external systems,
 * databases, and services.
 */
@ActivityInterface
public interface WorkflowActivities {

    /**
     * Fetch workflow hooks from database for a specific event type
     *
     * @param eventType Event type to fetch hooks for
     * @return List of workflow hooks
     */
    @ActivityMethod
    List<Map<String, Object>> fetchWorkflowHooks(String eventType);

    /**
     * Evaluate JEXL expression and return boolean result
     *
     * @param context Data context for expression evaluation
     * @param expression JEXL expression to evaluate
     * @return Boolean result of evaluation
     */
    @ActivityMethod
    Boolean evaluateExpression(Map<String, Object> context, String expression);

    /**
     * Evaluate JEXL expression and return result (any type)
     *
     * @param context Data context for expression evaluation
     * @param expression JEXL expression to evaluate
     * @return Result of evaluation
     */
    @ActivityMethod
    Object evaluateExpressionForResult(Map<String, Object> context, String expression);

    /**
     * Execute HTTP webhook to external system
     *
     * @param url URL to call
     * @param method HTTP method (GET, POST, PUT, etc.)
     * @param headers HTTP headers
     * @param payload Request payload
     * @return Response from webhook
     */
    @ActivityMethod
    Map<String, Object> executeWebhook(String url, String method,
                                        Map<String, String> headers, Object payload);

    /**
     * Capture policy snapshot by calling mock policy system
     *
     * @param agreementData Agreement data to snapshot
     * @param endpoint Endpoint to call
     * @param includeEndorsement Whether to include endorsement data
     * @return Snapshot result with ID and timestamp
     */
    @ActivityMethod
    Map<String, Object> capturePolicySnapshot(Map<String, Object> agreementData,
                                               String endpoint,
                                               Boolean includeEndorsement);

    /**
     * Persist workflow execution result to database
     *
     * @param workflowId Workflow ID
     * @param runId Run ID
     * @param eventType Event type
     * @param inputData Input data
     * @param resultData Result data
     */
    @ActivityMethod
    void persistWorkflowResult(String workflowId, String runId, String eventType,
                                Map<String, Object> inputData, Map<String, Object> resultData);
}
