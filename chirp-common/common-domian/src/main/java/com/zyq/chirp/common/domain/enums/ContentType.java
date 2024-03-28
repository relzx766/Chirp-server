package com.zyq.chirp.common.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ContentType {
    OCTET_STREAM("application/octet-stream");
    private final String type;
}
