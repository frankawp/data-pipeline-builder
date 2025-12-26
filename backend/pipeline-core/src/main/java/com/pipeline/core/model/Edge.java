package com.pipeline.core.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Pipeline 边 - 表示节点之间的连接
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Edge {

    private String id;
    private String sourceNodeId;
    private String targetNodeId;

    /**
     * 源节点输出端口
     */
    private String sourceHandle;

    /**
     * 目标节点输入端口
     */
    private String targetHandle;
}
