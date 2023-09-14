package com.zyq.chirp.gateway.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@Slf4j
public class WebsocketFilterFactory extends AbstractGatewayFilterFactory<Object> {
    public final static String WEBSOCKET_PATH = "/interaction";


    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            log.info(path);
            if (path.contains(WEBSOCKET_PATH)) {
                String token = path.substring(path.lastIndexOf(WEBSOCKET_PATH) + WEBSOCKET_PATH.length() + 1);
                path = path.substring(0, path.indexOf(WEBSOCKET_PATH) + WEBSOCKET_PATH.length());
                String userId = (String) StpUtil.getLoginIdByToken(token);
                if (userId == null) {
                    throw new NotLoginException("无效的token", StpUtil.getLoginType(), "未登录");
                }
                path = path + "/" + userId;
                URI newURI = UriComponentsBuilder.fromPath(path).build().toUri();
                log.info(newURI.toString());
                request = request.mutate().uri(newURI).build();
                exchange = exchange.mutate().request(request).build();
            }
            return chain.filter(exchange);
        };
    }
}
