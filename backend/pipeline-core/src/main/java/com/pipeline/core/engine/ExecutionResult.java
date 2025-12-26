package com.pipeline.core.engine;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline 执行结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionResult {

    private String executionId;
    private String pipelineId;
    private ExecutionContext.ExecutionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long totalRecordsProcessed;

    @Builder.Default
    private List<NodeResult> nodeResults = new ArrayList<>();

    private String errorMessage;

    public Duration getDuration() {
        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime);
        }
        return Duration.ZERO;
    }

    public boolean isSuccess() {
        return status == ExecutionContext.ExecutionStatus.COMPLETED;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeResult {
        private String nodeId;
        private String nodeName;
        private long recordsRead;
        private long recordsWritten;
        private long durationMs;
        private ExecutionContext.ExecutionStatus status;
        private String errorMessage;
    }
}
