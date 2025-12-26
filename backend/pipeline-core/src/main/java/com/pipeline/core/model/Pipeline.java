package com.pipeline.core.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pipeline 定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pipeline {

    private String id;
    private String name;
    private String description;

    @Builder.Default
    private List<Node> nodes = new ArrayList<>();

    @Builder.Default
    private List<Edge> edges = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    @Builder.Default
    private PipelineStatus status = PipelineStatus.DRAFT;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Node getNodeById(String nodeId) {
        return nodes.stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    public List<Edge> getEdgesFromNode(String nodeId) {
        return edges.stream()
                .filter(e -> e.getSourceNodeId().equals(nodeId))
                .toList();
    }

    public List<Edge> getEdgesToNode(String nodeId) {
        return edges.stream()
                .filter(e -> e.getTargetNodeId().equals(nodeId))
                .toList();
    }

    public List<Node> getSourceNodes() {
        return nodes.stream()
                .filter(n -> n.getType() == NodeType.SOURCE)
                .toList();
    }

    public List<Node> getTargetNodes() {
        return nodes.stream()
                .filter(n -> n.getType() == NodeType.TARGET)
                .toList();
    }

    public enum PipelineStatus {
        DRAFT,
        ACTIVE,
        INACTIVE,
        ARCHIVED
    }
}
