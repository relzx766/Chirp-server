package com.zyq.chirp.mediaserver.controller;

import com.zyq.chirp.common.exception.FileExistsException;
import com.zyq.chirp.common.model.enumration.HttpHeader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestControllerAdvice
public class ExceptionController {
    @ExceptionHandler(FileExistsException.class)
    public ResponseEntity<Object> fileExistsHandler(FileExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header(HttpHeader.MESSAGE.name(), Base64.getEncoder().encodeToString(e.getMessage().getBytes(StandardCharsets.UTF_8)))
                .body(e.getData());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> exHandler(Exception e) {
        e.printStackTrace();
        return ResponseEntity.badRequest()
                .header(HttpHeader.MESSAGE.name(),
                        Base64.getEncoder().encodeToString(e.getMessage().getBytes(StandardCharsets.UTF_8)))
                .build();
    }
}
