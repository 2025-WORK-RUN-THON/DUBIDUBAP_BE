package com.guineafigma.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache logosongList = new CaffeineCache(
                "logosong:list",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(60))
                        .maximumSize(1_000)
                        .build()
        );

        CaffeineCache logosongPopular = new CaffeineCache(
                "logosong:popular",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(60))
                        .maximumSize(1_000)
                        .build()
        );

        CaffeineCache quickStatus = new CaffeineCache(
                "logosong:quickStatus",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(3))
                        .maximumSize(100_000)
                        .build()
        );

        CaffeineCache sunoStatus = new CaffeineCache(
                "suno:status",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofSeconds(5))
                        .maximumSize(100_000)
                        .build()
        );

        CaffeineCache byId = new CaffeineCache(
                "logosong:byId",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .maximumSize(100_000)
                        .build()
        );

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(logosongList, logosongPopular, quickStatus, sunoStatus, byId));
        return manager;
    }
}


