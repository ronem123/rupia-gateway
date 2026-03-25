/**
 * Author: Ram Mandal
 * Created on @System: Apple M1 Pro
 * User:rammandal
 * Date:23/01/2026
 * Time:17:00
 */


package com.ronem.gateway.filter;


import com.ronem.gateway.exception.JwtTokenInvalidException;
import com.ronem.gateway.exception.JwtTokenMissingException;
import com.ronem.rupiasecuritylib.constants.HeaderUtil;
import com.ronem.rupiasecuritylib.constants.PublicPaths;
import com.ronem.rupiasecuritylib.enums.UserRole;
import com.ronem.rupiasecuritylib.properties.JwtProperties;
import com.ronem.rupiasecuritylib.service.JwtUtil;
import com.ronem.rupiasecuritylib.util.UserRoleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final JwtProperties jwtProperties;
    private final JwtUtil jwtUtil;
    private final UserRoleUtil userRoleUtil;


    // Headers to remove from client (prevent header spoofing)
    public static final List<String> BLACK_LISTED_HEADERS = Arrays.asList(
            HeaderUtil.xUserId,
            HeaderUtil.xUserId,
            HeaderUtil.xUserMobile,
            HeaderUtil.xUserRole,
            HeaderUtil.xInternalSecret
    );


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();


        log.info("GlobalFilter: {} {}", method, path);
        log.info("Adding internal secret header to request {}", exchange.getRequest().getURI());

        // remove the blacklisted headers from client first
        ServerHttpRequest.Builder requestBuilder = request.mutate();
        requestBuilder.headers(httpHeaders ->
                BLACK_LISTED_HEADERS.forEach(httpHeaders::remove)
        );

        // check if the path is public
        if (PublicPaths.isPublicPath(path)) {
            //add secrete to the header
            ServerHttpRequest modifiedRequest = requestBuilder
                    .header(HeaderUtil.xInternalSecret, jwtProperties.getAccessSecret())
                    .build();
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }

        // protect path - extract and validate JWT
        Mono<ServerHttpRequest> serverHttpRequestMono = Mono.fromCallable(() -> {
            String token = jwtUtil.extractToken(request);
            if (token == null) {
                log.warn("Missing JWT token for protected path: {}", path);
                throw new JwtTokenMissingException("Authentication token is expired");
            }

            if (!jwtUtil.validateAccessToken(token)) {
                log.warn("Invalid JWT token for path: {}", path);
                throw new JwtTokenInvalidException("Invalid or expired authentication token");
            }

            Long userId = jwtUtil.getSubject(token);
            String role = jwtUtil.getRole(token);

            String email = null;
            String mobileNumber = null;
            UserRole userRole = userRoleUtil.getMappedUserRole(role);
            if (userRole.equals(UserRole.CUSTOMER)) {
                mobileNumber = jwtUtil.getClaimMobileNumber(token);
            } else {
                email = jwtUtil.getClaimEmail(token);
            }
            log.info("Authenticated: userId={}, role={}, path={}", userId, userRole, path);

            // Build request with headers from gateway to consuming microservices
            return requestBuilder
                    .header(HeaderUtil.xInternalSecret, jwtProperties.getAccessSecret())
                    .header(HeaderUtil.xUserId, userId.toString())
                    .header(HeaderUtil.xUserEmail, email != null ? email : "")
                    .header(HeaderUtil.xUserMobile, mobileNumber != null ? mobileNumber : "")
                    .header(HeaderUtil.xUserRole, userRole.name())
                    .build();
        });

        // Continue filter chain with authenticated request
        return serverHttpRequestMono.flatMap(modifiedRequest ->
                        chain.filter(exchange.mutate().request(modifiedRequest).build())
                )
                .onErrorResume(JwtTokenMissingException.class, Mono::error)
                .onErrorResume(JwtTokenInvalidException.class, Mono::error)
                .onErrorResume(Exception.class, ex -> {
                    log.error("Unexpected error in JWT filter: {}", ex.getMessage());
                    throw new JwtTokenInvalidException("Authentication Failed:" + ex.getMessage());
                });
    }

    @Override
    public int getOrder() {
        return -100;
    }
}