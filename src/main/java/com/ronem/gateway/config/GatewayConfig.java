/**
 * Author: Ram Mandal
 * Created on @System: Apple M1 Pro
 * User:rammandal
 * Date:20/03/2026
 * Time:10:48
 */


package com.ronem.gateway.config;

import com.ronem.rupiasecuritylib.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
// here we enabled because this file reads the value from application.yaml
// It tells spring to create bean from JwtProperties class and bind values from application.yaml to bean
@EnableConfigurationProperties(JwtProperties.class)
@ComponentScan(
        basePackages = {
                "com.ronem.rupiasecuritylib.util",
                "com.ronem.rupiasecuritylib.enums",
                "com.ronem.rupiasecuritylib.constants",
                "com.ronem.rupiasecuritylib.service"
        }
)
public class GatewayConfig {
    // We only included what we need for Gateway
    // We can't include the GatewayAuthenticationFilter as this filter is web servlet based
    // and our Gateway is reactive with WebFlux

}