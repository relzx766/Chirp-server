package com.zyq.chirp.common.domain.exception;


import com.zyq.chirp.common.domain.model.Code;

public class ChirpException extends RuntimeException {
    private int code;

    public ChirpException(String message) {
        super(message);
    }

    public ChirpException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChirpException(Throwable cause) {
        super(cause);
    }

    public ChirpException(Code code, String message) {
        super(message);
        this.code = code.getCode();
    }

    public ChirpException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code.getCode();
    }

    public ChirpException(Code code, Throwable cause) {
        super(cause);
        this.code = code.getCode();
    }
}
