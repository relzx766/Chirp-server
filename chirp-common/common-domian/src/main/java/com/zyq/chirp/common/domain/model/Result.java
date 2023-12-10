package com.zyq.chirp.common.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    public Result put(String key, Object value) {
        Optional.ofNullable(this.data).ifPresentOrElse(info -> data.put(key, value),
                () -> {
                    data = new HashMap<>();
                    data.put(key, value);
                });
        return this;
    }
}
