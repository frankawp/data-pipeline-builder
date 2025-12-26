package com.pipeline.core.transformer;

/**
 * 转换器异常
 */
public class TransformerException extends RuntimeException {

    public TransformerException(String message) {
        super(message);
    }

    public TransformerException(String message, Throwable cause) {
        super(message, cause);
    }
}
