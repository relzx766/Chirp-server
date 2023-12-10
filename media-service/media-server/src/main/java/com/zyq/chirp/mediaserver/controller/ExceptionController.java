package com.zyq.chirp.mediaserver.controller;

import com.zyq.chirp.common.domain.exception.FileExistsException;
import com.zyq.chirp.mediaclient.dto.MediaDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionController {
    @ExceptionHandler(FileExistsException.class)
    public ResponseEntity<MediaDto> fileExistHandler(FileExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body((MediaDto) e.getData());
    }
}
