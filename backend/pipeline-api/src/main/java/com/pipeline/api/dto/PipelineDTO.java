package com.pipeline.api.dto;

import com.pipeline.core.model.Edge;
import com.pipeline.core.model.Node;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineDTO {

    private String id;

    @NotBlank(message = "Pipeline name is required")
    private String name;

    private String description;

    private List<Node> nodes;

    private List<Edge> edges;

    private Map<String, Object> variables;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
