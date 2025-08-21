package com.guineafigma.domain.logosong.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guineafigma.domain.logosong.dto.fastapi.GenerateResponseDto;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fastapi.base-url:http://127.0.0.1:8010}")
    private String fastapiBaseUrl;

    public GenerateResponseDto fetchGenerate(LogoSongCreateRequest req, String requestId) {
        String url = fastapiBaseUrl + "/api/v1/generate";
        Map<String, Object> body = new HashMap<>();
        body.put("service_name", req.getServiceName());
        body.put("slogan", Optional.ofNullable(req.getSlogan()).orElse(""));
        body.put("target_customer", Optional.ofNullable(req.getTargetCustomer()).orElse(""));
        // mood_tone: 콤마 구분 문자열 → 리스트
        List<String> moodList = new ArrayList<>();
        if (req.getMoodTone() != null && !req.getMoodTone().isBlank()) {
            for (String s : req.getMoodTone().split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) moodList.add(trimmed);
            }
        }
        if (moodList.isEmpty()) moodList = List.of();
        body.put("mood_tone", moodList);
        body.put("music_genre", req.getMusicGenre());
        body.put("version", req.getVersion().name());
        body.put("industry", Optional.ofNullable(req.getIndustry()).orElse(""));
        body.put("marketing_item", Optional.ofNullable(req.getMarketingItem()).orElse(""));
        body.put("extra", Optional.ofNullable(req.getAdditionalInfo()).orElse(""));
        body.put("generate_in", "fastapi");
        body.put("request_id", requestId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Request-Id", requestId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        log.info("FastAPI /generate 호출: url={}, requestId={}", url, requestId);
        GenerateResponseDto resp = restTemplate.postForObject(url, entity, GenerateResponseDto.class);
        try {
            if (resp != null) {
                int examplesCount = resp.getExamples() != null ? resp.getExamples().size() : 0;
                GenerateResponseDto.Analysis analysis = resp.getAnalysis();
                String bpm = (analysis != null && analysis.getMusicSummary() != null && analysis.getMusicSummary().getBpm() != null)
                        ? String.format("%.2f", analysis.getMusicSummary().getBpm()) : "n/a";
                String key = (analysis != null && analysis.getMusicSummary() != null) ? analysis.getMusicSummary().getKey() : null;
                String mode = (analysis != null && analysis.getMusicSummary() != null) ? analysis.getMusicSummary().getMode() : null;
                GenerateResponseDto.SunoRequestBase base = resp.getSunoRequestBase() != null ? resp.getSunoRequestBase() : resp.getSuno_request();
                Double styleWeight = base != null ? base.getStyleWeight() : null;
                Double weirdness = base != null ? base.getWeirdnessConstraint() : null;
                Double audioWeight = base != null ? base.getAudioWeight() : null;
                int masterLen = resp.getMaster_prompt() != null ? resp.getMaster_prompt().length() : 0;
                log.info(
                        "FastAPI /generate 응답 요약: requestId={}, masterPromptLen={}, examples={}, musicSummary(bpm={}, key={}, mode={}), weights(style={}, weirdness={}, audio={})",
                        resp.getRequestId(), masterLen, examplesCount, bpm, key, mode, styleWeight, weirdness, audioWeight
                );
            } else {
                log.warn("FastAPI /generate 응답이 null: requestId={}", requestId);
            }
        } catch (Exception e) {
            log.warn("FastAPI /generate 응답 로깅 중 예외 발생: {}", e.getMessage());
        }
        return resp;
    }
}


