package com.example.godisConnector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOrigin("*"); // 允许所有来源访问
        corsConfig.addAllowedMethod("*"); // 允许所有HTTP方法
        corsConfig.addAllowedHeader("*"); // 允许所有请求头

        // 设置 Access-Control-Expose-Headers 允许 Content-Disposition 被前端读取
        List<String> exposedHeaders = Arrays.asList("Content-Disposition");
        corsConfig.setExposedHeaders(exposedHeaders);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsFilter((CorsConfigurationSource) source);
    }
}