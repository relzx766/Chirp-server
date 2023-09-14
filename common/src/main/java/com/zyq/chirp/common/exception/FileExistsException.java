package com.zyq.chirp.common.exception;

public class FileExistsException extends RuntimeException {
    private Object data;

    public FileExistsException() {
        super("文件已存在");
    }

    public FileExistsException(Object data) {
        super("文件已存在");
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
