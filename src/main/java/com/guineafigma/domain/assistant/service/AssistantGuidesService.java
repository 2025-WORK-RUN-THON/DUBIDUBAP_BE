package com.guineafigma.domain.assistant.service;

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
public class AssistantGuidesService {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${openai.api.key}")
	private String openaiApiKey;

	@Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
	private String openaiApiUrl;

	@Value("${openai.api.model:gpt-4o-mini}")
	private String openaiModel;

	public GuidesResponse generateGuides(LogoSongCreateRequest req) {
		try {
			String masterPrompt = buildMasterPrompt(req);

			String content = null;
			try {
				content = callOpenAI(masterPrompt, openaiModel);
			} catch (HttpClientErrorException.BadRequest e) {
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
			if (content == null || content.isEmpty()) throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);

			JsonNode json = objectMapper.readTree(content);
			String lyrics = json.path("lyrics").asText("");
			String videoGuide = json.path("video_guideline").asText("");

			return GuidesResponse.builder()
					.lyrics(lyrics)
					.videoGuideline(videoGuide)
					.promptUsed(masterPrompt)
					.build();
		} catch (Exception e) {
			log.error("가이드 생성 실패", e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	private String callOpenAI(String masterPrompt, String model) {
		Map<String, Object> body = new HashMap<>();
		body.put("model", model);
		body.put("temperature", 0.7);
		body.put("messages", List.of(
				Map.of("role", "system", "content", "You are an elite brand jingle producer and a video creative director. Output strictly in valid JSON."),
				Map.of("role", "user", "content", masterPrompt)
		));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(openaiApiKey);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

		String response = restTemplate.postForObject(openaiApiUrl, entity, String.class);
		if (response == null) throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		try {
			JsonNode root = objectMapper.readTree(response);
			return root.path("choices").path(0).path("message").path("content").asText("");
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	private String buildMasterPrompt(LogoSongCreateRequest r) {
		String tone = r.getMoodTone() == null ? "" : r.getMoodTone();
		StringBuilder sb = new StringBuilder();
		sb.append("You must return a compact JSON with keys: lyrics (Korean jingle lyrics), video_guideline (Korean, concise scene-by-scene guide). No extra text.\\n");
		sb.append("Follow modern prompt best practices: be explicit, include constraints, and structure outputs.\\n");
		sb.append("Constraints: lyrics max ~1200 chars, catchy hook, repeat service name >=2 times, slogan in chorus if provided. Video: 15/30/60s depending on version; no human faces; one product-centric visual motif.\\n");
		sb.append("Inputs: \\n");
		sb.append("- service_name: ").append(nullToEmpty(r.getServiceName())).append("\\n");
		sb.append("- slogan: ").append(nullToEmpty(r.getSlogan())).append("\\n");
		sb.append("- industry: ").append(nullToEmpty(r.getIndustry())).append("\\n");
		sb.append("- marketing_item: ").append(nullToEmpty(r.getMarketingItem())).append("\\n");
		sb.append("- target_customer: ").append(nullToEmpty(r.getTargetCustomer())).append("\\n");
		sb.append("- mood_tone: ").append(tone).append("\\n");
		sb.append("- music_genre: ").append(r.getMusicGenre()).append("\\n");
		sb.append("- version: ").append(r.getVersion()).append(" (SHORT=~15s, LONG=~45-60s)\\n");
		sb.append("- additional_info: ").append(nullToEmpty(r.getAdditionalInfo())).append("\\n");
		sb.append("Tasks: 1) Write Korean lyrics tailored to inputs. 2) Create Korean video_guideline with 4-8 bullet scenes matching length. 3) Keep JSON minimal.\\n");
		return sb.toString();
	}

	private String nullToEmpty(String s) { return s == null ? "" : s; }
}


