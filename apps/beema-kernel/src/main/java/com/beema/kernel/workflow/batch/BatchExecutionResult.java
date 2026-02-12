package com.beema.kernel.workflow.batch;

import java.util.Date;

public record BatchExecutionResult(
        Long jobExecutionId,
        String jobName,
        String status,
        Date startTime,
        Date endTime,
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
        return endTime.getTime() - startTime.getTime();
    }
}
