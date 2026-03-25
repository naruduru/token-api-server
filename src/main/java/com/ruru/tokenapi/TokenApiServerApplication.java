package com.ruru.tokenapi;

import com.ruru.tokenapi.config.TokenApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TokenApiProperties.class)
public class TokenApiServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TokenApiServerApplication.class, args);
    }
}
