package com.zyq.chirp.adviceserver.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class WsMessageEntity {
    String request;
    String content;
}
