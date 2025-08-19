package com.guineafigma.domain.logosong.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "가사 및 비디오 가이드라인 생성 응답 - Suno API 연동을 위한 텍스트 콘텐츠 (URL 없음)")
public class GuidesResponse {

	@Schema(description = "OpenAI가 생성한 로고송 가사 - Suno API customMode에서 prompt로 사용될 텍스트", example = "카페 뒤비뒤밥에서\n따뜻한 커피 한 잔\n달콤한 케이크와 함께\n행복한 시간")
	private String lyrics;

	@Schema(description = "OpenAI가 생성한 비디오 제작 가이드라인 - 영상 제작자를 위한 가이드 텍스트", example = "카페 내부 전경, 커피 제작 과정, 고객들의 웃는 모습, 따뜻한 조명과 나무 인테리어 강조")
	private String videoGuideline;

	@Schema(description = "AI 생성에 사용된 최종 프롬프트 (디버깅 및 품질 검증용)", example = "Create a logo song lyrics for a cozy cafe named Dubidubap...")
	private String promptUsed;
}


