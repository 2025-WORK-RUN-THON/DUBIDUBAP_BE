package com.guineafigma.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class RetryConfig {
    // @Retryable 어노테이션 활성화
}