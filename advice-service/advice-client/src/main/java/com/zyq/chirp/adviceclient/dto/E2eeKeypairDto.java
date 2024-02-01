package com.zyq.chirp.adviceclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class E2eeKeypairDto {
    private String prime;
    private String generator;
}
