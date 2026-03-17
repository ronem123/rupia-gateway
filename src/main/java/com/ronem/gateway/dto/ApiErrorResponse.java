/**
 * Author: Ram Mandal
 * Created on @System: Apple M1 Pro
 * User:rammandal
 * Date:27/01/2026
 * Time:15:46
 */


package com.ronem.gateway.dto;

import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Builder
public record ApiErrorResponse(Boolean status,
                               HttpStatus errorCode,
                               String message,
                               Instant time) {
}