/**
 * Author: Ram Mandal
 * Created on @System: Apple M1 Pro
 * User:rammandal
 * Date:17/03/2026
 * Time:09:37
 */


package com.ronem.gateway.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class JwtAuthenticationException extends RuntimeException {
    private final HttpStatus status;

    public JwtAuthenticationException(String message) {
        super(message);
        this.status = HttpStatus.UNAUTHORIZED;
    }

    public JwtAuthenticationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}