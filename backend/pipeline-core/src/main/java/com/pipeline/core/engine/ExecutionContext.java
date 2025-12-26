package com.pipeline.core.engine;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pipeline 执行上下文
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionContext {

    @Builder.Default
    private String executionId = UUID.randomUUID().toString();

    private String pipelineId;

    @Builder.Default
    private LocalDateTime startTime = LocalDateTime.now();

    private LocalDateTime endTime;

    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    @Builder.Default
    private Map<String, NodeExecutionStats> nodeStats = new HashMap<>();

    private String errorMessage;

    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    public void recordNodeStats(String nodeId, NodeExecutionStats stats) {
        nodeStats.put(nodeId, stats);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeExecutionStats {
        private String nodeId;
        private long recordsProcessed;
        private long durationMs;
        private ExecutionStatus status;
        private String errorMessage;
    }

    public enum ExecutionStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
