package com.pipeline.core.model;

/**
 * 节点类型
 */
public enum NodeType {
    /**
     * 数据源节点 - 读取数据
     */
    SOURCE,

    /**
     * 转换节点 - 处理数据
     */
    TRANSFORMER,

    /**
     * 目标节点 - 写入数据
     */
    TARGET
}
