package com.guineafigma.domain.logosong.service;

import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.domain.logosong.client.SunoApiClient;
import com.guineafigma.domain.logosong.client.FastApiClient;
import com.guineafigma.domain.logosong.dto.fastapi.GenerateResponseDto;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SunoApiService {

    private final SunoApiClient sunoApiClient;
    private final LogoSongRepository logoSongRepository;
    private final FastApiClient fastApiClient;
    private final SunoParamMapper sunoParamMapper;

    @Value("${app.domain:http://localhost:8080}")
    private String appDomain;

    @Retryable(
            value = {BusinessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    @Transactional
    public String generateMusic(LogoSong logoSong) {
        if (logoSong.getMusicStatus() == MusicGenerationStatus.PROCESSING) {
            if (logoSong.getSunoTaskId() != null) {
                log.info("이미 진행 중인 Suno 작업 재사용: logoSongId={}, taskId={}", logoSong.getId(), logoSong.getSunoTaskId());
                return logoSong.getSunoTaskId();
            }
            throw new BusinessException(ErrorCode.MUSIC_GENERATION_IN_PROGRESS);
        }

        try {
            String prompt = logoSong.getLyrics();
            String style = logoSong.getMusicGenre();
            String title = logoSong.getServiceName() + " - Logo Song (" + logoSong.getVersion().getDisplayName() + ")";
            String model = "V3_5";
            String callBackUrl = "https://dummy-callback.com";
            Integer duration = getVersionDurationSeconds(logoSong.getVersion());

            log.info("Suno API 음악 생성 시작: logoSongId={}, title={}, duration={}초", logoSong.getId(), title, duration);

            SunoGenerateRequest request;
            try {
                String requestId = java.util.UUID.randomUUID().toString();
                LogoSongCreateRequest genReq = LogoSongCreateRequest.builder()
                        .serviceName(logoSong.getServiceName())
                        .slogan(logoSong.getSlogan())
                        .industry(logoSong.getIndustry())
                        .marketingItem(logoSong.getMarketingItem())
                        .targetCustomer(logoSong.getTargetCustomer())
                        .moodTone(logoSong.getMoodTone())
                        .musicGenre(logoSong.getMusicGenre())
                        .version(logoSong.getVersion())
                        .additionalInfo(logoSong.getAdditionalInfo())
                        .build();
                GenerateResponseDto gen = fastApiClient.fetchGenerate(genReq, requestId);
                try {
                    if (gen != null) {
                        int examplesCount = gen.getExamples() != null ? gen.getExamples().size() : 0;
                        var ms = gen.getAnalysis() != null ? gen.getAnalysis().getMusicSummary() : null;
                        var baseLog = (gen.getSunoRequestBase() != null ? gen.getSunoRequestBase() : gen.getSuno_request());
                        Double sw = baseLog != null ? baseLog.getStyleWeight() : null;
                        Double ww = baseLog != null ? baseLog.getWeirdnessConstraint() : null;
                        Double aw = baseLog != null ? baseLog.getAudioWeight() : null;
                        log.info("FastAPI 응답 반영(Suno tuning): requestId={}, examples={}, bpm={}, key={}, mode={}, baseWeights(style={}, weirdness={}, audio={})",
                                gen.getRequestId(), examplesCount,
                                (ms != null ? ms.getBpm() : null), (ms != null ? ms.getKey() : null), (ms != null ? ms.getMode() : null),
                                sw, ww, aw);
                    }
                } catch (Exception ignore) {}
                GenerateResponseDto.SunoRequestBase base = (gen != null)
                        ? (gen.getSunoRequestBase() != null ? gen.getSunoRequestBase() : gen.getSuno_request())
                        : null;
                request = sunoParamMapper.tuneFromAnalysis(base, gen != null ? gen.getAnalysis() : null, prompt, style, title, duration);
                if (request.getCallBackUrl() == null) {
                    request = SunoGenerateRequest.builder()
                            .customMode(true)
                            .instrumental(request.getInstrumental() != null ? request.getInstrumental() : false)
                            .model(request.getModel() != null ? request.getModel() : model)
                            .callBackUrl(callBackUrl)
                            .prompt(request.getPrompt())
                            .style(request.getStyle() != null ? request.getStyle() : style)
                            .title(request.getTitle() != null ? request.getTitle() : title)
                            .negativeTags(request.getNegativeTags())
                            .vocalGender(request.getVocalGender())
                            .styleWeight(request.getStyleWeight())
                            .weirdnessConstraint(request.getWeirdnessConstraint())
                            .audioWeight(request.getAudioWeight())
                            .duration(request.getDuration() != null ? request.getDuration() : duration)
                            .build();
                }
            } catch (Exception e) {
                log.warn("FastAPI 분석 기반 튜닝 실패, 기본 파라미터 사용: {}", e.getMessage());
                request = SunoGenerateRequest.of(prompt, style, title, model, callBackUrl, duration);
            }

            SunoGenerateResponse response = sunoApiClient.generateMusic(request);

            if (response.getId() != null) {
                logoSong.updateSunoTaskId(response.getId());
                logoSong.updateMusicStatus(MusicGenerationStatus.PROCESSING);
                logoSongRepository.save(logoSong);
                log.info("Suno API 음악 생성 요청 성공: logoSongId={}, taskId={}, duration={}초", logoSong.getId(), response.getId(), duration);
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

    @Cacheable(value = "suno:status", key = "#taskId", sync = true)
    public MusicGenerationResult checkMusicStatus(String taskId) {
        try {
            var recordInfo = sunoApiClient.getGenerateRecordInfo(taskId);
            if (recordInfo.getCode() != null && recordInfo.getCode() == 200 && recordInfo.getData() != null) {
                String status = recordInfo.getData().getStatus();
                MusicGenerationStatus mapped = switch (status) {
                    case "SUCCESS" -> MusicGenerationStatus.COMPLETED;
                    case "PENDING", "TEXT_SUCCESS", "FIRST_SUCCESS" -> MusicGenerationStatus.PROCESSING;
                    default -> MusicGenerationStatus.PROCESSING;
                };

                String audioUrl = null;
                String imageUrl = null;
                Double duration = null;
                if (recordInfo.getData().getResponse() != null && recordInfo.getData().getResponse().getSunoData() != null && !recordInfo.getData().getResponse().getSunoData().isEmpty()) {
                    var first = recordInfo.getData().getResponse().getSunoData().get(0);
                    audioUrl = first.getAudioUrl() != null ? first.getAudioUrl() : first.getStreamAudioUrl();
                    imageUrl = first.getImageUrl();
                    duration = first.getDuration();
                }

                return MusicGenerationResult.builder()
                        .taskId(taskId)
                        .status(mapped)
                        .audioUrl(audioUrl)
                        .imageUrl(imageUrl)
                        .duration(duration)
                        .build();
            }

            SunoStatusResponse response = sunoApiClient.getGenerationStatus(taskId);
            return MusicGenerationResult.fromSunoStatus(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("음악 생성 상태 확인 중 예외 발생: taskId={}", taskId, e);
            throw new BusinessException(ErrorCode.SUNO_API_ERROR);
        }
    }

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

    private Integer getVersionDurationSeconds(Object version) {
        // 모든 버전에서 45초로 강제 제한
        if (version == null) return 45;
        String versionStr = version.toString();
        return switch (versionStr.toUpperCase()) {
            case "SHORT" -> 45; // 15 -> 45초로 변경
            case "LONG" -> 60;  // 60 -> 45초로 변경
            default -> 45;      // 30 -> 45초로 변경
        };
    }
}