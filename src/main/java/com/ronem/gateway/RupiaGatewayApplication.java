package com.ronem.gateway;

import com.ronem.rupiasecuritylib.config.SecurityLibConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDiscoveryClient
public class RupiaGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(RupiaGatewayApplication.class, args);
    }

}
