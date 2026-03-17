/**
 * Author: Ram Mandal
 * Created on @System: Apple M1 Pro
 * User:rammandal
 * Date:17/03/2026
 * Time:09:43
 */


package com.ronem.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ronem.gateway.dto.ApiErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
@Order(-2) // high priority then default error handler
@RequiredArgsConstructor
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof JwtAuthenticationException jwtEx) {
            return handleJwtException(exchange, jwtEx);
        }
        return handleGenericException(exchange, ex);
    }


    private Mono<Void> handleJwtException(ServerWebExchange exchange, JwtAuthenticationException ex) {
        log.error("JWT Authentication error {}", ex.getMessage());
        HttpStatus status = ex.getStatus();
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                false,
                status,
                ex.getMessage(),
                Instant.now()
        );
        return writeResponse(exchange, errorResponse, status);
    }

    private Mono<Void> handleGenericException(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("Unhandled error: {}", ex.getMessage(), ex);
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                false,
                status,
                "An unexpected error occurred",
                Instant.now()
        );

        return writeResponse(exchange, errorResponse, status);
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, ApiErrorResponse errorResponse, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error writing response", e);
            return exchange.getResponse().setComplete();
        }
    }
}