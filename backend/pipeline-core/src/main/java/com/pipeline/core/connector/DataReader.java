package com.pipeline.core.connector;

import com.pipeline.core.model.DataRecord;
import com.pipeline.core.model.DataSchema;

import java.util.Iterator;

/**
 * 数据读取器接口
 */
public interface DataReader extends AutoCloseable {

    /**
     * 获取数据结构
     */
    DataSchema getSchema();

    /**
     * 流式读取数据
     */
    Iterator<DataRecord> read();

    /**
     * 预估数据量
     */
    long estimateCount();

    /**
     * 打开连接
     */
    void open();

    /**
     * 关闭连接
     */
    @Override
    void close();
}
