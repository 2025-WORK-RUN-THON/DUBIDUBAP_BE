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
@Schema(description = "가사 및 비디오 가이드라인 생성 응답")
public class GuidesResponse {

	@Schema(description = "생성된 로고송 가사")
	private String lyrics;

	@Schema(description = "생성된 비디오 가이드라인")
	private String videoGuideline;

	@Schema(description = "생성에 사용된 프롬프트 (디버그용)")
	private String promptUsed;
}


