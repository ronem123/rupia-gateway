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
import com.ronem.rupiasecuritylib.enums.UserRole;
import com.ronem.rupiasecuritylib.properties.JwtProperties;
import com.ronem.rupiasecuritylib.service.JwtUtil;
import com.ronem.rupiasecuritylib.util.HeaderUtil;
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

    //public path that don't need JWT
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/admin/login",
            "/api/customer/register"
    );

    // Headers to remove from client (prevent header spoofing)
    public static final List<String> BLACK_LISTED_HEADERS = Arrays.asList(
            HeaderUtil.xUserId,
            HeaderUtil.xUserId,
            HeaderUtil.xUserMobile,
            HeaderUtil.xUserRole,
            HeaderUtil.xInternalSecret
    );

    private boolean isPublicPath(String path) {
        //anyMatch will consume predicate and return boolean
        //here path::startsWith is something like this (method reference)
        //anyMatch(value->path.startsWith(value)) here (value) -> path.startsWith(value) is lamda expression
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();


        log.info("GlobalFilter: {} {}", method, path);

        // 1. remove the blacklisted headers from client first
        ServerHttpRequest.Builder requestBuilder = request.mutate();
        requestBuilder.headers(httpHeaders ->
                BLACK_LISTED_HEADERS.forEach(httpHeaders::remove)
        );

//        TestClass test = new TestClass();
//        Float sum = test.calculate(2, 3, (a, b) -> Float.intBitsToFloat(a + b));

        // 2. check if the path is public
        if (isPublicPath(path)) {
            //add secrete to the header
            ServerHttpRequest modifiedRequest = requestBuilder
                    .header(HeaderUtil.xInternalSecret, jwtProperties.getAccessSecret())
                    .build();
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }

        // 3. protect path - extract and validate JWT
        return Mono.fromCallable(() -> {
                    String token = jwtUtil.extractToken(request);
                    if (token == null) {
                        log.warn("Missing JWT token for protected path: {}", path);
                        throw new JwtTokenMissingException("Authentication token is expired");
                    }

                    if (!jwtUtil.validateAccessToken(token)) {
                        log.warn("Invalid JWT token for path: {}", path);
                        throw new JwtTokenInvalidException("Invalid or expired authentication token");
                    }

                    //extract claims from token
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
                })
                .flatMap(modifiedRequest ->
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