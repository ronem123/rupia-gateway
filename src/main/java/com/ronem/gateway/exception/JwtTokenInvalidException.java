/**
 * Author: Ram Mandal
 * Created on @System: Apple M1 Pro
 * User:rammandal
 * Date:17/03/2026
 * Time:09:41
 */


package com.ronem.gateway.exception;

public class JwtTokenInvalidException extends JwtAuthenticationException {
    public JwtTokenInvalidException(String message) {
        super(message);
    }
}