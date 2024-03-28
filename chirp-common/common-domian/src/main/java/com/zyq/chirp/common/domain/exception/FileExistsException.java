package com.zyq.chirp.common.domain.exception;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FileExistsException extends RuntimeException {
    private Object data;

    public FileExistsException() {
        super("文件已存在");
    }

    public FileExistsException(Object data) {
        super("文件已存在");
        this.data = data;
    }

}
