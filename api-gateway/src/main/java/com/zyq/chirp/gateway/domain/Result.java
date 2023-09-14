package com.zyq.chirp.gateway.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatusCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Result {
    private Integer code;
    private Map<String, Object> data;
    private String message;

    public static Result ok() {
        return new Result(Code.OK.getCode(), null, "ok");
    }

    public static Result ok(Map<String, Object> data) {
        return new Result(Code.OK.getCode(), null, "ok");
    }

    public static Result err() {
        return new Result(Code.ERR.getCode(), null, "err");
    }

    public static Result err(String message) {
        return new Result(Code.ERR.getCode(), null, message);
    }

    public void setCode(HttpStatusCode code) {
        this.code = code.value();
    }

    public Result put(String key, Object value) {
        Optional.ofNullable(this.data).ifPresentOrElse(info -> data.put(key, value),
                () -> {
                    data = new HashMap<>();
                    data.put(key, value);
                });
        return this;
    }
}
