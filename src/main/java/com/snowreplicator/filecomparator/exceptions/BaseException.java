package com.snowreplicator.filecomparator.exceptions;

public abstract class BaseException extends RuntimeException {
    public BaseException(String message) {
        super(message);
    }
}
