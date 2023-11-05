package com.zyq.chirp.authserver.controller;

import com.zyq.chirp.common.model.enumration.HttpHeader;
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
