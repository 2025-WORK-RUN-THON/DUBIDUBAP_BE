package com.guineafigma.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.S3Client;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    @Profile("test")
    public S3Client testS3Client() {
        return mock(S3Client.class);
    }
    
    @Bean
    @Profile("test")
    @Primary
    public RestTemplate testRestTemplate() {
        // 외부 API 호출을 막기 위한 Mock RestTemplate
        return mock(RestTemplate.class);
    }
}