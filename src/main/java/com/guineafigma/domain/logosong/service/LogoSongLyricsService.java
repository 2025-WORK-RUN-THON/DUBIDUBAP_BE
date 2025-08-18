package com.guineafigma.domain.logosong.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.GuidesResponse;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoSongLyricsService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String openaiModel;

    public GuidesResponse generateLyricsAndVideoGuide(LogoSongCreateRequest request) {
        try {
            log.info("OpenAI API 호출 시작 - 모델: {}, API URL: {}", openaiModel, openaiApiUrl);
            String masterPrompt = buildAdvancedPrompt(request);

            String content = null;
            try {
                content = callOpenAI(masterPrompt, openaiModel);
                log.info("OpenAI API 호출 성공");
            } catch (HttpClientErrorException.BadRequest e) {
                log.error("OpenAI API BadRequest 에러: {}", e.getResponseBodyAsString());
                String body = e.getResponseBodyAsString();
                if (body != null && (body.contains("invalid model") || body.contains("does not exist"))) {
                    for (String fallback : List.of("gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo")) {
                        try {
                            content = callOpenAI(masterPrompt, fallback);
                            openaiModel = fallback;
                            log.warn("OpenAI 모델 폴백 적용: {} -> {}", "invalid model", fallback);
                            break;
                        } catch (Exception ignore) {
                            content = null;
                        }
                    }
                    if (content == null) throw e;
                } else {
                    throw e;
                }
            }
            
            if (content == null || content.isEmpty()) {
                throw new BusinessException(ErrorCode.LYRICS_GENERATION_FAILED);
            }

            // OpenAI 응답이 마크다운 코드 블록으로 감싸져 있을 경우 JSON 부분만 추출
            String jsonContent = extractJsonFromMarkdown(content);
            log.debug("추출된 JSON: {}", jsonContent);
            
            JsonNode json = objectMapper.readTree(jsonContent);
            String lyrics = json.path("lyrics").asText("");
            String videoGuide = json.path("video_guideline").asText("");

            if (lyrics.isEmpty()) {
                throw new BusinessException(ErrorCode.LYRICS_GENERATION_FAILED);
            }

            return GuidesResponse.builder()
                    .lyrics(lyrics)
                    .videoGuideline(videoGuide)
                    .promptUsed(masterPrompt)
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("가사 및 비디오 가이드라인 생성 실패 - 상세 에러: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.LYRICS_GENERATION_FAILED);
        }
    }

    private String callOpenAI(String masterPrompt, String model) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.8); // 창의성을 위해 조금 더 높게 설정
        body.put("max_tokens", 2000);
        body.put("messages", List.of(
                Map.of("role", "system", "content", buildSystemPrompt()),
                Map.of("role", "user", "content", masterPrompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String response = restTemplate.postForObject(openaiApiUrl, entity, String.class);
        if (response == null) {
            throw new BusinessException(ErrorCode.LYRICS_GENERATION_FAILED);
        }
        
        try {
            log.debug("OpenAI 원본 응답: {}", response);
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            log.debug("OpenAI 추출된 컨텐츠: {}", content);
            return content;
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패 - 원본 응답: '{}', 에러: {}", response, e.getMessage(), e);
            throw new BusinessException(ErrorCode.LYRICS_GENERATION_FAILED);
        }
    }

    private String extractJsonFromMarkdown(String content) {
        // 마크다운 코드 블록으로 감싸진 JSON 추출
        if (content.startsWith("```")) {
            // ```json 이나 ``` 으로 시작하는 경우
            int startIndex = content.indexOf('\n') + 1;
            int endIndex = content.lastIndexOf("```");
            if (startIndex > 0 && endIndex > startIndex) {
                return content.substring(startIndex, endIndex).trim();
            }
        }
        
        // 백틱으로만 감싸진 경우
        if (content.startsWith("`") && content.endsWith("`")) {
            return content.substring(1, content.length() - 1).trim();
        }
        
        // JSON 객체 시작과 끝을 찾아서 추출
        int jsonStart = content.indexOf('{');
        int jsonEnd = content.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return content.substring(jsonStart, jsonEnd + 1);
        }
        
        // 그외의 경우 원본 그대로 반환
        return content;
    }

    private String buildSystemPrompt() {
        return """
                당신은 세계적인 브랜드 징글 작곡가이자 영상 크리에이티브 디렉터입니다.
                
                역할:
                1. 브랜드 로고송 가사 작성 전문가
                2. 음악 비디오 컨셉 기획 전문가
                3. 마케팅 효과를 극대화하는 창작자
                
                출력 규칙:
                - 반드시 유효한 JSON 형태로만 응답
                - 한국어로 작성
                - 가사는 기억하기 쉽고 중독성 있게
                - 비디오 가이드라인은 구체적이고 실행 가능하게
                """;
    }

    private String buildAdvancedPrompt(LogoSongCreateRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        // Chain-of-Thought 프롬프트 엔지니어링 적용
        prompt.append("# 브랜드 로고송 & 영상 제작 가이드 생성\n\n");
        
        // 1단계: 브랜드 분석
        prompt.append("## 1단계: 브랜드 분석\n");
        prompt.append("서비스명: ").append(nullToEmpty(request.getServiceName())).append("\n");
        prompt.append("슬로건: ").append(nullToEmpty(request.getSlogan())).append("\n");
        prompt.append("업종: ").append(nullToEmpty(request.getIndustry())).append("\n");
        prompt.append("마케팅 포인트: ").append(nullToEmpty(request.getMarketingItem())).append("\n");
        prompt.append("타겟 고객: ").append(nullToEmpty(request.getTargetCustomer())).append("\n\n");
        
        // 2단계: 음악적 특성 분석
        prompt.append("## 2단계: 음악적 특성 분석\n");
        prompt.append("장르: ").append(request.getMusicGenre()).append("\n");
        prompt.append("무드/톤: ").append(nullToEmpty(request.getMoodTone())).append("\n");
        prompt.append("버전: ").append(request.getVersion()).append(" (");
        prompt.append(getVersionDescription(request.getVersion())).append(")\n");
        prompt.append("추가 정보: ").append(nullToEmpty(request.getAdditionalInfo())).append("\n\n");
        
        // 3단계: 작업 지시사항
        prompt.append("## 3단계: 작업 지시사항\n\n");
        
        prompt.append("### 가사 작성 요구사항:\n");
        prompt.append("- 서비스명 '").append(request.getServiceName()).append("' 최소 2회 반복\n");
        if (request.getSlogan() != null && !request.getSlogan().isEmpty()) {
            prompt.append("- 슬로건 '").append(request.getSlogan()).append("' 후렴구에 자연스럽게 포함\n");
        }
        prompt.append("- ").append(request.getMusicGenre()).append(" 장르에 맞는 리듬감\n");
        prompt.append("- 기억하기 쉬운 후크 라인 포함\n");
        prompt.append("- 브랜드 정체성을 강화하는 키워드 활용\n");
        prompt.append("- 약 ").append(getVersionDuration(request.getVersion())).append(" 분량\n\n");
        
        prompt.append("### 영상 가이드라인 요구사항:\n");
        prompt.append("- ").append(getVersionDuration(request.getVersion())).append(" 영상 구성\n");
        prompt.append("- 인물 얼굴 노출 금지 (손, 실루엣은 가능)\n");
        prompt.append("- 제품/서비스 중심의 시각적 모티프\n");
        prompt.append("- 브랜드 컬러와 조화로운 색감\n");
        prompt.append("- 씬별 구체적인 촬영 가이드\n");
        prompt.append("- ").append(request.getMusicGenre()).append(" 장르에 맞는 비주얼 스타일\n\n");
        
        // 4단계: 출력 형식
        prompt.append("## 4단계: 출력 형식\n");
        prompt.append("다음 JSON 형식으로 정확히 응답하세요:\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"lyrics\": \"완성된 로고송 가사 (한국어, 줄바꿈 포함)\",\n");
        prompt.append("  \"video_guideline\": \"구체적인 영상 제작 가이드라인 (한국어, 씬별 설명)\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("위 요구사항을 모두 반영하여 창의적이고 마케팅 효과가 높은 로고송 가사와 영상 가이드라인을 작성해주세요.");
        
        return prompt.toString();
    }

    private String getVersionDescription(Object version) {
        if (version == null) return "표준";
        String versionStr = version.toString();
        return switch (versionStr.toUpperCase()) {
            case "SHORT" -> "15초 내외의 짧은 버전";
            case "LONG" -> "45-60초의 긴 버전";
            default -> "30초 표준 버전";
        };
    }

    private String getVersionDuration(Object version) {
        if (version == null) return "30초";
        String versionStr = version.toString();
        return switch (versionStr.toUpperCase()) {
            case "SHORT" -> "15초";
            case "LONG" -> "60초";
            default -> "30초";
        };
    }

    private String nullToEmpty(String str) {
        return str == null ? "" : str;
    }
}