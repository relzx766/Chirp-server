package com.zyq.chirp.mediaserver.controller;

import com.zyq.chirp.common.exception.FileExistsException;
import com.zyq.chirp.common.model.enumration.HttpHeader;
import com.zyq.chirp.mediaclient.dto.MediaDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@RestControllerAdvice
public class ExceptionController {
    @ExceptionHandler(FileExistsException.class)
    public ResponseEntity<MediaDto> fileExistHandler(FileExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body((MediaDto) e.getData());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> argumentNotValidHandler(MethodArgumentNotValidException e) {
        e.printStackTrace();
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        StringBuilder sb = new StringBuilder();
        for (ObjectError error : allErrors) {
            sb.append(error.getDefaultMessage()).append(";\n");
        }
        return this.buildRes(sb.toString());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> exHandler(Exception e) {
        e.printStackTrace();
        return this.buildRes(e.getMessage());
    }

    public ResponseEntity<String> buildRes(String message) {
        return ResponseEntity.badRequest()
                .header(HttpHeader.MESSAGE.name(),
                        Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8)))
                .build();
    }
}
