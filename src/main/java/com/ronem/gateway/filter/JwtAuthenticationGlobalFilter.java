/**
 * Author: Ram Mandal
 * Created on @System: Apple M1 Pro
 * User:rammandal
 * Date:23/01/2026
 * Time:17:00
 */


package com.ronem.gateway.filter;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Slf4j
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        log.info("GlobalFilter: {} {}", method, path);
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null) {

        } else {

        }
        return chain.filter(exchange)
                .doOnSuccess(aVoid ->
                        log.info("GlobalFilter: Request completed: {} {}", method, path)
                )
                .doOnError(error ->
                        log.error("GlobalFilter: Request failed : {} {}, Reason={} ", method, path, error.getMessage())
                );
    }

    @Override
    public int getOrder() {
        return -1;
    }
}