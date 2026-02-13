package com.beema.kernel.workflow.submission;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Map;

/**
 * Temporal workflow interface for the quote submission lifecycle.
 *
 * Orchestrates the flow: DRAFT -> Rating -> QUOTED -> Wait for BIND signal -> BOUND.
 * The workflow starts when a submission is created via the API and waits
 * for an external bind signal to finalize the submission.
 */
@WorkflowInterface
public interface SubmissionWorkflow {

    /**
     * Execute the submission workflow.
     *
     * @param submissionId unique identifier for this submission
     * @param product      the product type (e.g., "gadget")
     * @param data         submission form data used for rating
     * @return the submissionId upon completion
     */
    @WorkflowMethod
    String execute(String submissionId, String product, Map<String, Object> data);

    /**
     * Signal the workflow to bind the quoted submission.
     * Transitions the submission from QUOTED to BOUND status.
     */
    @SignalMethod
    void bind();
}
