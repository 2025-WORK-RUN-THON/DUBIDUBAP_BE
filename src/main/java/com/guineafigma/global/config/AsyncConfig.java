package com.guineafigma.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "musicGenerationExecutor")
    public Executor musicGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 코어 스레드 수
        executor.setCorePoolSize(2);
        
        // 최대 스레드 수
        executor.setMaxPoolSize(5);
        
        // 큐 용량
        executor.setQueueCapacity(100);
        
        // 스레드 이름 접두사
        executor.setThreadNamePrefix("MusicGen-");
        
        // 스레드 유지 시간 (초)
        executor.setKeepAliveSeconds(60);
        
        // 애플리케이션 종료 시 스레드 풀 종료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 종료 대기 시간 (초)
        executor.setAwaitTerminationSeconds(30);
        
        // 큐가 가득 찬 경우 정책 (호출자 스레드에서 실행)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 스레드 팩토리 설정
        executor.setThreadFactory(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        });
        
        // 태스크 데코레이터 설정 (로깅)
        executor.setTaskDecorator(runnable -> () -> {
            String threadName = Thread.currentThread().getName();
            log.debug("음악 생성 비동기 작업 시작: thread={}", threadName);
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("음악 생성 비동기 작업 실패: thread={}", threadName, e);
                throw e;
            } finally {
                log.debug("음악 생성 비동기 작업 종료: thread={}", threadName);
            }
        });
        
        executor.initialize();
        
        log.info("Music Generation Thread Pool 초기화 완료: core={}, max={}, queue={}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Async-");
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        log.info("기본 Task Executor 초기화 완료");
        
        return executor;
    }
}