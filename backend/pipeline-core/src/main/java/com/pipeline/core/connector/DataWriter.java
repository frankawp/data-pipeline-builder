package com.pipeline.core.connector;

import com.pipeline.core.model.DataRecord;
import com.pipeline.core.model.DataSchema;

import java.util.Iterator;

/**
 * 数据写入器接口
 */
public interface DataWriter extends AutoCloseable {

    /**
     * 设置目标数据结构
     */
    void setSchema(DataSchema schema);

    /**
     * 打开连接
     */
    void open();

    /**
     * 写入单条记录
     */
    void write(DataRecord record);

    /**
     * 批量写入
     */
    void write(Iterator<DataRecord> records);

    /**
     * 提交事务
     */
    void commit();

    /**
     * 回滚事务
     */
    void rollback();

    /**
     * 关闭连接
     */
    @Override
    void close();

    /**
     * 获取已写入记录数
     */
    long getWrittenCount();
}
