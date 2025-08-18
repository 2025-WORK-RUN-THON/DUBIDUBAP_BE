package com.guineafigma.domain.logosong.service;

import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.domain.logosong.client.SunoApiClient;
import com.guineafigma.domain.logosong.dto.request.SunoCallbackRequest;
import com.guineafigma.domain.logosong.dto.request.SunoGenerateRequest;
import com.guineafigma.domain.logosong.dto.response.MusicGenerationResult;
import com.guineafigma.domain.logosong.dto.response.SunoGenerateResponse;
import com.guineafigma.domain.logosong.dto.response.SunoStatusResponse;
import com.guineafigma.domain.logosong.entity.LogoSong;
import com.guineafigma.domain.logosong.repository.LogoSongRepository;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SunoApiService {

    private final SunoApiClient sunoApiClient;
    private final LogoSongRepository logoSongRepository;

    @Retryable(
            value = {BusinessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public String generateMusic(LogoSong logoSong) {
        if (logoSong.getMusicStatus() == MusicGenerationStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.MUSIC_GENERATION_IN_PROGRESS);
        }

        try {
            // 고급 프롬프트 엔지니어링 적용
            String prompt = buildAdvancedMusicPrompt(logoSong);
            String tags = buildMusicTags(logoSong);
            String title = buildMusicTitle(logoSong);
            String model = selectOptimalModel(logoSong);

            SunoGenerateRequest request = SunoGenerateRequest.of(prompt, tags, title, model);
            
            log.info("Suno API 음악 생성 시작: logoSongId={}, title={}", logoSong.getId(), title);
            
            SunoGenerateResponse response = sunoApiClient.generateMusic(request);
            
            if (response.getId() != null) {
                // LogoSong 상태 업데이트
                logoSong.updateSunoTaskId(response.getId());
                logoSong.updateMusicStatus(MusicGenerationStatus.PROCESSING);
                logoSongRepository.save(logoSong);
                
                log.info("Suno API 음악 생성 요청 성공: logoSongId={}, taskId={}", logoSong.getId(), response.getId());
                return response.getId();
            } else {
                throw new BusinessException(ErrorCode.MUSIC_GENERATION_FAILED);
            }
        } catch (BusinessException e) {
            logoSong.updateMusicStatus(MusicGenerationStatus.FAILED);
            logoSongRepository.save(logoSong);
            throw e;
        } catch (Exception e) {
            log.error("음악 생성 중 예외 발생: logoSongId={}", logoSong.getId(), e);
            logoSong.updateMusicStatus(MusicGenerationStatus.FAILED);
            logoSongRepository.save(logoSong);
            throw new BusinessException(ErrorCode.MUSIC_GENERATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public MusicGenerationResult checkMusicStatus(String taskId) {
        try {
            SunoStatusResponse response = sunoApiClient.getGenerationStatus(taskId);
            return MusicGenerationResult.fromSunoStatus(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("음악 생성 상태 확인 중 예외 발생: taskId={}", taskId, e);
            throw new BusinessException(ErrorCode.SUNO_API_ERROR);
        }
    }

    @Transactional
    public void handleMusicGenerationCallback(String taskId, MusicGenerationResult result) {
        try {
            LogoSong logoSong = logoSongRepository.findBySunoTaskId(taskId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SUNO_TASK_NOT_FOUND));

            logoSong.updateMusicStatus(result.getStatus());
            
            if (result.getStatus() == MusicGenerationStatus.COMPLETED) {
                logoSong.updateGeneratedMusicUrl(result.getAudioUrl());
                log.info("음악 생성 완료: logoSongId={}, taskId={}, audioUrl={}", 
                        logoSong.getId(), taskId, result.getAudioUrl());
            } else if (result.getStatus() == MusicGenerationStatus.FAILED) {
                log.error("음악 생성 실패: logoSongId={}, taskId={}, error={}", 
                        logoSong.getId(), taskId, result.getErrorMessage());
            }
            
            logoSongRepository.save(logoSong);
        } catch (Exception e) {
            log.error("음악 생성 콜백 처리 중 예외 발생: taskId={}", taskId, e);
        }
    }

    /**
     * 고급 프롬프트 엔지니어링 기법을 적용한 음악 생성 프롬프트 구축
     */
    private String buildAdvancedMusicPrompt(LogoSong logoSong) {
        StringBuilder prompt = new StringBuilder();

        // Chain-of-Thought 프롬프트 엔지니어링
        prompt.append("Create a professional brand jingle with the following specifications:\n\n");

        // 브랜드 정보 구조화
        prompt.append("BRAND IDENTITY:\n");
        prompt.append("- Service: ").append(logoSong.getServiceName()).append("\n");
        if (logoSong.getSlogan() != null && !logoSong.getSlogan().isEmpty()) {
            prompt.append("- Slogan: ").append(logoSong.getSlogan()).append("\n");
        }
        if (logoSong.getIndustry() != null && !logoSong.getIndustry().isEmpty()) {
            prompt.append("- Industry: ").append(logoSong.getIndustry()).append("\n");
        }
        if (logoSong.getTargetCustomer() != null && !logoSong.getTargetCustomer().isEmpty()) {
            prompt.append("- Target: ").append(logoSong.getTargetCustomer()).append("\n");
        }

        // 음악적 특성 정의
        prompt.append("\nMUSICAL REQUIREMENTS:\n");
        prompt.append("- Genre: ").append(logoSong.getMusicGenre()).append("\n");
        prompt.append("- Mood: ").append(logoSong.getMoodTone() != null ? logoSong.getMoodTone() : "upbeat and memorable").append("\n");
        prompt.append("- Duration: ").append(getVersionDuration(logoSong.getVersion())).append("\n");
        prompt.append("- BPM: ").append(calculateBPM(logoSong.getMusicGenre())).append("\n");
        prompt.append("- Key: ").append(suggestKey(logoSong.getMoodTone())).append("\n");

        // 가사 통합 (Few-Shot Learning 적용)
        if (logoSong.getLyrics() != null && !logoSong.getLyrics().isEmpty()) {
            prompt.append("\nLYRICS:\n");
            prompt.append(logoSong.getLyrics()).append("\n");
        }

        // 제약사항 및 품질 기준
        prompt.append("\nCONSTRAINTS:\n");
        prompt.append("- Must repeat service name '").append(logoSong.getServiceName()).append("' at least 2 times\n");
        prompt.append("- Commercial quality production\n");
        prompt.append("- Memorable and catchy melody\n");
        prompt.append("- Clear vocal delivery\n");
        prompt.append("- Professional mixing and mastering\n");

        // 장르별 특화 지시사항
        prompt.append("\nSTYLE DIRECTION:\n");
        prompt.append(getGenreSpecificDirection(logoSong.getMusicGenre()));

        return prompt.toString();
    }

    private String buildMusicTags(LogoSong logoSong) {
        StringBuilder tags = new StringBuilder();
        
        // 기본 장르 태그
        tags.append(logoSong.getMusicGenre().toString().toLowerCase());
        
        // 무드 태그 추가
        if (logoSong.getMoodTone() != null && !logoSong.getMoodTone().isEmpty()) {
            tags.append(", ").append(logoSong.getMoodTone().toLowerCase());
        }
        
        // 브랜드 징글 특화 태그
        tags.append(", commercial, jingle, brand music, memorable");
        
        // 업종별 태그 추가
        if (logoSong.getIndustry() != null && !logoSong.getIndustry().isEmpty()) {
            tags.append(", ").append(logoSong.getIndustry().toLowerCase());
        }
        
        // 버전별 태그
        tags.append(", ").append(logoSong.getVersion().toString().toLowerCase());
        
        return tags.toString();
    }

    private String buildMusicTitle(LogoSong logoSong) {
        return logoSong.getServiceName() + " - Logo Song (" + logoSong.getVersion() + ")";
    }

    private String selectOptimalModel(LogoSong logoSong) {
        // 버전과 장르에 따른 최적 모델 선택
        String version = logoSong.getVersion().toString();
        String genre = logoSong.getMusicGenre().toString();
        
        // 고품질이 필요한 경우 V4 사용
        if ("LONG".equals(version) || "JAZZ".equals(genre) || "CLASSICAL".equals(genre)) {
            return "v4";
        }
        
        // 일반적인 경우 V3_5 사용 (비용 효율적)
        return "v3_5";
    }

    private String getVersionDuration(Object version) {
        if (version == null) return "30 seconds";
        String versionStr = version.toString();
        return switch (versionStr.toUpperCase()) {
            case "SHORT" -> "15 seconds";
            case "LONG" -> "60 seconds";
            default -> "30 seconds";
        };
    }

    private int calculateBPM(Object genre) {
        if (genre == null) return 120;
        String genreStr = genre.toString().toUpperCase();
        return switch (genreStr) {
            case "ELECTRONIC", "DANCE" -> 128;
            case "ROCK", "POP" -> 120;
            case "JAZZ" -> 100;
            case "BALLAD" -> 80;
            case "HIP_HOP" -> 90;
            case "CLASSICAL" -> 60;
            default -> 120;
        };
    }

    private String suggestKey(String moodTone) {
        if (moodTone == null) return "C Major";
        String mood = moodTone.toLowerCase();
        if (mood.contains("밝") || mood.contains("긍정") || mood.contains("활기")) {
            return "C Major";
        } else if (mood.contains("따뜻") || mood.contains("부드")) {
            return "F Major";
        } else if (mood.contains("고급") || mood.contains("세련")) {
            return "G Major";
        } else if (mood.contains("차분") || mood.contains("안정")) {
            return "D Major";
        }
        return "C Major";
    }

    private String getGenreSpecificDirection(Object genre) {
        if (genre == null) return "Create a modern, versatile commercial jingle";
        
        String genreStr = genre.toString().toUpperCase();
        return switch (genreStr) {
            case "POP" -> "Modern pop production with catchy hooks, clear vocals, and radio-friendly mix";
            case "ROCK" -> "Energetic rock with driving rhythm, electric guitars, and powerful vocals";
            case "ELECTRONIC" -> "Clean electronic production with synthesizers, clear beats, and modern sound design";
            case "JAZZ" -> "Smooth jazz arrangement with live instruments feel, sophisticated harmony";
            case "BALLAD" -> "Emotional ballad with piano/strings, intimate vocal delivery, slower tempo";
            case "HIP_HOP" -> "Modern hip-hop beat with clear vocals, rhythmic delivery, urban feel";
            case "CLASSICAL" -> "Orchestral arrangement with traditional instruments, elegant composition";
            case "DANCE" -> "Upbeat dance track with electronic elements, energetic rhythm, club-ready sound";
            default -> "Create a modern, versatile commercial jingle with professional production quality";
        };
    }
}