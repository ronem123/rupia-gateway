/**
 * Author: Ram Mandal
 * Created on @System: Apple M1 Pro
 * User:rammandal
 * Date:17/03/2026
 * Time:09:40
 */


package com.ronem.gateway.exception;

public class JwtTokenMissingException extends JwtAuthenticationException {
    public JwtTokenMissingException(String message) {
        super(message);
    }
}