package com.ruru.tokenapi.config;

import com.ruru.tokenapi.auth.PartnerApiTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final PartnerApiTokenInterceptor partnerApiTokenInterceptor;

    public WebConfig(PartnerApiTokenInterceptor partnerApiTokenInterceptor) {
        this.partnerApiTokenInterceptor = partnerApiTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(partnerApiTokenInterceptor)
            .addPathPatterns("/api/internal/**")
            .excludePathPatterns("/api/internal/token", "/api/internal/token/refresh");
    }
}
