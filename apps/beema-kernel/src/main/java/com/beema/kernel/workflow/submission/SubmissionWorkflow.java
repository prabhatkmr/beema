package com.beema.kernel.workflow.submission;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.Map;

/**
 * Durable workflow for managing insurance submission lifecycle.
 *
 * Lifecycle: DRAFT → QUOTE signal → QUOTED → BIND signal → BOUND → Policy Created
 *
 * This workflow replaces traditional job/batch processing with a durable,
 * event-driven approach that maintains state across async operations.
 */
@WorkflowInterface
public interface SubmissionWorkflow {

    /**
     * Main workflow entry point.
     * Creates submission in DRAFT state and waits for signals.
     *
     * @param args Submission creation arguments
     * @return Final submission status
     */
    @WorkflowMethod
    String execute(SubmissionStartArgs args);

    /**
     * Signal to request a quote.
     * Triggers rating engine calculation and transitions to QUOTED state.
     */
    @SignalMethod
    void quote();

    /**
     * Signal to bind the submission and create a policy.
     * Only valid in QUOTED state.
     */
    @SignalMethod
    void bind();

    /**
     * Query current submission status.
     *
     * @return Current status (DRAFT, QUOTED, BOUND, ERROR)
     */
    @QueryMethod
    String getStatus();

    /**
     * Query calculated premium (available after QUOTE signal).
     *
     * @return Premium amount, or null if not yet quoted
     */
    @QueryMethod
    Double getQuotedPremium();

    /**
     * Query created policy number (available after BIND signal).
     *
     * @return Policy number, or null if not yet bound
     */
    @QueryMethod
    String getPolicyNumber();
}
