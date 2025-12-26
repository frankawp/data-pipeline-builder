package com.pipeline.api.controller;

import com.pipeline.api.dto.PipelineDTO;
import com.pipeline.api.entity.ExecutionEntity;
import com.pipeline.api.service.PipelineService;
import com.pipeline.core.engine.ExecutionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pipelines")
@RequiredArgsConstructor
@Tag(name = "Pipeline", description = "Pipeline management APIs")
@CrossOrigin(origins = "*")
public class PipelineController {

    private final PipelineService pipelineService;

    @GetMapping
    @Operation(summary = "Get all pipelines")
    public ResponseEntity<List<PipelineDTO>> getAllPipelines() {
        return ResponseEntity.ok(pipelineService.getAllPipelines());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pipeline by ID")
    public ResponseEntity<PipelineDTO> getPipeline(@PathVariable String id) {
        return ResponseEntity.ok(pipelineService.getPipeline(id));
    }

    @PostMapping
    @Operation(summary = "Create a new pipeline")
    public ResponseEntity<PipelineDTO> createPipeline(@Valid @RequestBody PipelineDTO dto) {
        return ResponseEntity.ok(pipelineService.createPipeline(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update pipeline")
    public ResponseEntity<PipelineDTO> updatePipeline(
            @PathVariable String id,
            @Valid @RequestBody PipelineDTO dto) {
        return ResponseEntity.ok(pipelineService.updatePipeline(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete pipeline")
    public ResponseEntity<Void> deletePipeline(@PathVariable String id) {
        pipelineService.deletePipeline(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Execute pipeline")
    public ResponseEntity<ExecutionResult> executePipeline(@PathVariable String id) {
        return ResponseEntity.ok(pipelineService.executePipeline(id));
    }

    @GetMapping("/{id}/executions")
    @Operation(summary = "Get pipeline execution history")
    public ResponseEntity<List<ExecutionEntity>> getExecutions(@PathVariable String id) {
        return ResponseEntity.ok(pipelineService.getExecutions(id));
    }
}
