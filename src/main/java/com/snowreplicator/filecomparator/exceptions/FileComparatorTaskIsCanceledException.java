package com.snowreplicator.filecomparator.exceptions;

public class FileComparatorTaskIsCanceledException extends BaseException {
    public FileComparatorTaskIsCanceledException() {
        super("File comparator task was canceled");
    }
}
