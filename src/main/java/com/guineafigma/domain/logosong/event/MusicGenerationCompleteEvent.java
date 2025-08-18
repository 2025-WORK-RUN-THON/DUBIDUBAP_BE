package com.guineafigma.domain.logosong.event;

import com.guineafigma.domain.logosong.dto.response.MusicGenerationResult;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MusicGenerationCompleteEvent extends ApplicationEvent {

    private final Long logoSongId;
    private final String taskId;
    private final MusicGenerationResult result;

    public MusicGenerationCompleteEvent(Object source, Long logoSongId, String taskId, MusicGenerationResult result) {
        super(source);
        this.logoSongId = logoSongId;
        this.taskId = taskId;
        this.result = result;
    }

    // source 없이 생성하는 편의 생성자
    public MusicGenerationCompleteEvent(Long logoSongId, String taskId, MusicGenerationResult result) {
        super(logoSongId); // logoSongId를 source로 사용
        this.logoSongId = logoSongId;
        this.taskId = taskId;
        this.result = result;
    }
}