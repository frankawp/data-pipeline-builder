package com.pipeline.core.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * Pipeline 节点
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Node {

    private String id;
    private String name;
    private NodeType type;

    /**
     * 连接器或转换器类型，如 mysql, postgresql, csv, filter, map 等
     */
    private String pluginType;

    /**
     * 节点配置
     */
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    /**
     * UI 位置信息
     */
    private Position position;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Position {
        private double x;
        private double y;
    }
}
