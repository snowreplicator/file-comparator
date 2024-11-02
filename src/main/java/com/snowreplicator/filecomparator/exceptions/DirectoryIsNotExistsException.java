package com.snowreplicator.filecomparator.exceptions;

public class DirectoryIsNotExistsException extends BaseException {
    public DirectoryIsNotExistsException(String dir) {
        super("Directory: '" + dir + "' is not exists");
    }
}
