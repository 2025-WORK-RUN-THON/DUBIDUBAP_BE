package com.guineafigma.common.enums;

public enum MoodTone {
    TRUSTWORTHY("Trustworthy", "신뢰감"),
    CHEERFUL("Cheerful", "경쾌함"),
    LUXURY("Luxury", "럭셔리"),
    HUMOROUS("Humorous", "유머러스");

    private final String name;
    private final String koreanName;

    MoodTone(String name, String koreanName) {
        this.name = name;
        this.koreanName = koreanName;
    }

    public String getName() {
        return name;
    }

    public String getKoreanName() {
        return koreanName;
    }
}