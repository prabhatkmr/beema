package com.beema.kernel.domain.submission;

/**
 * Submission lifecycle status.
 *
 * State transitions:
 * DRAFT -> QUOTED -> BOUND -> ISSUED
 *                 -> DECLINED
 */
public enum SubmissionStatus {
    DRAFT,
    QUOTED,
    BOUND,
    ISSUED,
    DECLINED
}
