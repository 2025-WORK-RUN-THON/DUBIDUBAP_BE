package com.guineafigma.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3Client;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    @Profile("test")
    public S3Client testS3Client() {
        return mock(S3Client.class);
    }
}