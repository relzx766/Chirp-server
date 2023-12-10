package com.zyq.chirp.gateway.filter;

import com.zyq.chirp.gateway.rewriter.JsonRewriteFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ResponseFilter implements GlobalFilter, Ordered {
    private final GatewayFilter delegate;

    @Autowired
    public ResponseFilter(ModifyResponseBodyGatewayFilterFactory factory, JsonRewriteFunction rewriteFunction) {
        delegate = factory.apply(new ModifyResponseBodyGatewayFilterFactory.Config()
                .setInClass(byte[].class)
                .setOutClass(byte[].class)
                .setRewriteFunction(rewriteFunction));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        });
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return delegate.filter(exchange, chain);
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
