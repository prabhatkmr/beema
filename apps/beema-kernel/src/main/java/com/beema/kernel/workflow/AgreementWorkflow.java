package com.beema.kernel.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Map;

/**
 * Agreement Workflow Interface
 *
 * Main Temporal workflow that acts as a DSL interpreter.
 * Fetches workflow hooks from database, evaluates trigger conditions,
 * and executes configured actions.
 */
@WorkflowInterface
public interface AgreementWorkflow {

    /**
     * Executes the agreement workflow based on event type and agreement data.
     *
     * @param eventType Event type (e.g., "agreement.created", "agreement.updated")
     * @param agreementData Agreement data to process
     * @return Workflow execution result with action results
     */
    @WorkflowMethod
    Map<String, Object> executeAgreementWorkflow(String eventType, Map<String, Object> agreementData);
}
