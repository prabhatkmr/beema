package com.beema.kernel.domain.submission;

/**
 * Submission lifecycle status.
 *
 * State transitions:
 * DRAFT -> QUOTED -> BOUND
 *                 -> DECLINED
 */
public enum SubmissionStatus {
    DRAFT,
    QUOTED,
    BOUND,
    DECLINED
}
