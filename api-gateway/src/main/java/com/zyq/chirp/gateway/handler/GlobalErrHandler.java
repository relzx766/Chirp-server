package com.zyq.chirp.gateway.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.common.domain.model.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@Order(-3)
public class GlobalErrHandler implements ErrorWebExceptionHandler {
    @Resource
    ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("全局异常处理:", ex);
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        Result result = new Result();
        result.setCode(response.getStatusCode().value());
        String message = ex.getMessage();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (ex instanceof NotLoginException || ex.getCause() instanceof NotLoginException) {
            response.setStatusCode(HttpStatus.OK);
            result.setCode(HttpStatus.UNAUTHORIZED.value());
            message = "请先登录";
        } else {
            response.setStatusCode(HttpStatus.OK);
        }
        result.setMessage(message);
        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            try {
                return bufferFactory.wrap(objectMapper.writeValueAsBytes(result));
            } catch (JsonProcessingException e) {
                log.error("全局异常处理异常:", e);
                return bufferFactory.wrap(new byte[0]);
            }
        }));

    }
}
