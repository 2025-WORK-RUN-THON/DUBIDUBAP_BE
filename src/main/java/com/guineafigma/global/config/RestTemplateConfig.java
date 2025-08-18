package com.guineafigma.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);   // 30초 - 연결 타임아웃
        factory.setReadTimeout(120000);     // 120초 (2분) - 읽기 타임아웃 (OpenAI API 복잡한 가사 생성용)
        return new RestTemplate(factory);
    }
}