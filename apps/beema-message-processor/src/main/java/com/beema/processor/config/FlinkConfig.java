package com.beema.processor.config;

import java.io.Serializable;

/**
 * Flink job configuration.
 */
public class FlinkConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String jobName;
    private final int parallelism;
    private final long checkpointInterval;

    public FlinkConfig(String jobName, int parallelism, long checkpointInterval) {
        this.jobName = jobName;
        this.parallelism = parallelism;
        this.checkpointInterval = checkpointInterval;
    }

    public String getJobName() {
        return jobName;
    }

    public int getParallelism() {
        return parallelism;
    }

    public long getCheckpointInterval() {
        return checkpointInterval;
    }

    public static FlinkConfig fromEnv() {
        String jobName = System.getenv().getOrDefault("FLINK_JOB_NAME", "beema-message-processor");
        int parallelism = Integer.parseInt(System.getenv().getOrDefault("FLINK_PARALLELISM", "1"));
        long checkpointInterval = Long.parseLong(
                System.getenv().getOrDefault("FLINK_CHECKPOINT_INTERVAL", "60000")
        );

        return new FlinkConfig(jobName, parallelism, checkpointInterval);
    }
}
