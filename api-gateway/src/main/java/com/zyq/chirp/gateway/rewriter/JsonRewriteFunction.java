package com.zyq.chirp.gateway.rewriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.common.domain.enums.HttpHeader;
import com.zyq.chirp.common.domain.model.Result;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
public class JsonRewriteFunction implements RewriteFunction<byte[], byte[]> {
    @Resource
    ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public Publisher<byte[]> apply(ServerWebExchange exchange, byte[] bytes) {
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        Optional<List<String>> message = Optional.ofNullable(exchange.getResponse().getHeaders().get(HttpHeader.MESSAGE.name()));
        Result result = new Result();
        result.setCode(status.value());
        message.ifPresent(m -> {
            byte[] msg = Base64.getDecoder().decode(m.get(0));
            result.setMessage(new String(msg, StandardCharsets.UTF_8));
        });
        if (bytes != null && bytes.length > 0) {
            result.put("record", objectMapper.readValue(bytes, JsonNode.class));
        }
        return Mono.just(objectMapper.writeValueAsBytes(result));
    }
}
