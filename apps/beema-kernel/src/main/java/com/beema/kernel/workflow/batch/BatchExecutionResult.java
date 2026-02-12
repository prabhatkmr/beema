package com.beema.kernel.workflow.batch;

import java.time.Duration;
import java.time.LocalDateTime;

public record BatchExecutionResult(
        Long jobExecutionId,
        String jobName,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        long readCount,
        long writeCount,
        long skipCount,
        String exitCode,
        String errorMessage
) {
    public boolean isSuccessful() {
        return "COMPLETED".equals(status);
    }

    public long getDurationMillis() {
        if (startTime == null || endTime == null) return 0;
        return Duration.between(startTime, endTime).toMillis();
    }
}
