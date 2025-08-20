package com.guineafigma.domain.logosong.service;

import com.guineafigma.domain.logosong.dto.fastapi.GenerateResponseDto;
import com.guineafigma.domain.logosong.dto.request.SunoGenerateRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SunoParamMapper {

    public SunoGenerateRequest tuneFromAnalysis(
            GenerateResponseDto.SunoRequestBase base,
            GenerateResponseDto.Analysis analysis,
            String lyrics,
            String style,
            String title,
            Integer durationSeconds
    ) {
        double styleSimilarity = estimateStyleSimilarity(style, analysis);
        double emotionEntropy = estimateEmotionEntropy(analysis != null ? analysis.getEmotionHint() : null);
        boolean isHighEnergy = isHighEnergy(analysis);
        boolean needBrandClarity = false; // 추후 브랜드 반복 요구 신호 연결

        double styleWeight = clamp01(round2(0.65 + 0.10 * styleSimilarity - 0.05 * emotionEntropy));
        double weirdness = clamp01(round2(0.35 + 0.10 * (isHighEnergy ? 1.0 : 0.0) - 0.05 * (needBrandClarity ? 1.0 : 0.0)));
        double audioWeight = clamp01(round2(0.65 + 0.10 * styleSimilarity)); // 임시: 오디오 유사도 대체

        SunoGenerateRequest req = SunoGenerateRequest.builder()
                .customMode(true)
                .instrumental(false)
                .model(base != null ? base.getModel() : "V3_5")
                .callBackUrl(base != null ? base.getCallBackUrl() : null)
                .prompt(lyrics)
                .style(style)
                .title(title)
                .negativeTags(base != null ? base.getNegativeTags() : null)
                .vocalGender(base != null ? base.getVocalGender() : null)
                .styleWeight(styleWeight)
                .weirdnessConstraint(weirdness)
                .audioWeight(audioWeight)
                .duration(durationSeconds)
                .build();

        log.info("SunoParam tuned: styleWeight={}, weirdness={}, audioWeight={}", styleWeight, weirdness, audioWeight);
        return req;
    }

    private static double estimateStyleSimilarity(String style, GenerateResponseDto.Analysis analysis) {
        if (style == null || analysis == null || analysis.getExamples() == null) return 0.5;
        String s = style.toLowerCase();
        int hits = 0;
        for (Map<String, String> ex : analysis.getExamples()) {
            String title = ex.getOrDefault("title", "").toLowerCase();
            if (!title.isEmpty() && title.contains(s)) hits++;
        }
        double score = Math.min(1.0, hits / 3.0);
        return score;
    }

    private static boolean isHighEnergy(GenerateResponseDto.Analysis analysis) {
        if (analysis == null) return false;
        GenerateResponseDto.MusicSummary m = analysis.getMusicSummary();
        if (m != null && m.getBpm() != null && m.getBpm() > 130) return true;
        Map<String, Double> emo = analysis.getEmotionHint();
        if (emo != null) {
            Double lively = maxOfKeys(emo, List.of("활기참", "energetic", "upbeat"));
            if (lively != null && lively > 0.3) return true;
        }
        return false;
    }

    private static Double maxOfKeys(Map<String, Double> map, List<String> keys) {
        if (map == null) return null;
        Double mx = null;
        for (String k : keys) {
            Double v = map.get(k);
            if (v != null) mx = mx == null ? v : Math.max(mx, v);
        }
        return mx;
    }

    private static double estimateEmotionEntropy(Map<String, Double> emo) {
        if (emo == null || emo.isEmpty()) return 0.5;
        double sum = emo.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= 0) return 0.5;
        double h = 0.0;
        for (double v : emo.values()) {
            double p = v / sum;
            if (p > 0) h += -p * Math.log(p);
        }
        // 정규화: 최대 엔트로피 ~ ln(N), N=emo size
        double maxH = Math.log(emo.size());
        if (maxH <= 0) return 0.5;
        return Math.max(0.0, Math.min(1.0, h / maxH));
    }

    private static double clamp01(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }

    private static double round2(double x) {
        return Math.round(x * 100.0) / 100.0;
    }
}


