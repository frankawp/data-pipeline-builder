package com.pipeline.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipeline.api.dto.PipelineDTO;
import com.pipeline.api.entity.ExecutionEntity;
import com.pipeline.api.entity.PipelineEntity;
import com.pipeline.api.repository.ExecutionRepository;
import com.pipeline.api.repository.PipelineRepository;
import com.pipeline.core.engine.ExecutionResult;
import com.pipeline.core.engine.PipelineExecutor;
import com.pipeline.core.model.Pipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineRepository pipelineRepository;
    private final ExecutionRepository executionRepository;
    private final PipelineExecutor pipelineExecutor;
    private final ObjectMapper objectMapper;

    public List<PipelineDTO> getAllPipelines() {
        return pipelineRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PipelineDTO getPipeline(String id) {
        return pipelineRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Pipeline not found: " + id));
    }

    @Transactional
    public PipelineDTO createPipeline(PipelineDTO dto) {
        String id = UUID.randomUUID().toString();
        dto.setId(id);
        dto.setStatus("DRAFT");

        PipelineEntity entity = toEntity(dto);
        pipelineRepository.save(entity);

        return toDTO(entity);
    }

    @Transactional
    public PipelineDTO updatePipeline(String id, PipelineDTO dto) {
        PipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline not found: " + id));

        dto.setId(id);
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setDefinition(serializePipeline(dto));
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }

        pipelineRepository.save(entity);
        return toDTO(entity);
    }

    @Transactional
    public void deletePipeline(String id) {
        pipelineRepository.deleteById(id);
    }

    @Transactional
    public ExecutionResult executePipeline(String id) {
        PipelineDTO dto = getPipeline(id);
        Pipeline pipeline = toPipeline(dto);

        // 记录执行开始
        ExecutionEntity execution = ExecutionEntity.builder()
                .id(UUID.randomUUID().toString())
                .pipelineId(id)
                .status("RUNNING")
                .startTime(LocalDateTime.now())
                .build();
        executionRepository.save(execution);

        try {
            // 执行 Pipeline
            ExecutionResult result = pipelineExecutor.execute(pipeline);

            // 更新执行记录
            execution.setStatus(result.getStatus().name());
            execution.setEndTime(result.getEndTime());
            execution.setRecordsProcessed(result.getTotalRecordsProcessed());
            execution.setResult(objectMapper.writeValueAsString(result));
            if (result.getErrorMessage() != null) {
                execution.setErrorMessage(result.getErrorMessage());
            }
            executionRepository.save(execution);

            return result;

        } catch (Exception e) {
            log.error("Pipeline execution failed", e);
            execution.setStatus("FAILED");
            execution.setEndTime(LocalDateTime.now());
            execution.setErrorMessage(e.getMessage());
            executionRepository.save(execution);
            throw new RuntimeException("Pipeline execution failed: " + e.getMessage(), e);
        }
    }

    public List<ExecutionEntity> getExecutions(String pipelineId) {
        return executionRepository.findByPipelineIdOrderByStartTimeDesc(pipelineId);
    }

    private PipelineDTO toDTO(PipelineEntity entity) {
        try {
            PipelineDTO dto = objectMapper.readValue(entity.getDefinition(), PipelineDTO.class);
            dto.setId(entity.getId());
            dto.setName(entity.getName());
            dto.setDescription(entity.getDescription());
            dto.setStatus(entity.getStatus());
            dto.setCreatedAt(entity.getCreatedAt());
            dto.setUpdatedAt(entity.getUpdatedAt());
            return dto;
        } catch (JsonProcessingException e) {
            // 如果解析失败，返回基本信息
            return PipelineDTO.builder()
                    .id(entity.getId())
                    .name(entity.getName())
                    .description(entity.getDescription())
                    .status(entity.getStatus())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }

    private PipelineEntity toEntity(PipelineDTO dto) {
        return PipelineEntity.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .definition(serializePipeline(dto))
                .status(dto.getStatus())
                .build();
    }

    private String serializePipeline(PipelineDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize pipeline", e);
        }
    }

    private Pipeline toPipeline(PipelineDTO dto) {
        return Pipeline.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .nodes(dto.getNodes())
                .edges(dto.getEdges())
                .variables(dto.getVariables())
                .build();
    }
}
