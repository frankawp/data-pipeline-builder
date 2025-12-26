package com.pipeline.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Pipeline 执行记录实体
 */
@Entity
@Table(name = "pipeline_executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "pipeline_id", length = 36, nullable = false)
    private String pipelineId;

    @Column(length = 20)
    private String status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "records_processed")
    private Long recordsProcessed;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String result;

    @Lob
    @Column(columnDefinition = "TEXT", name = "error_message")
    private String errorMessage;
}
