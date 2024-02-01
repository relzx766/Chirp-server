package com.zyq.chirp.authclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthDto {
    private String account;
    private String password;
    private String accountType;
    private String token;

    public void clearPwd() {
        this.password = "";
    }
}
