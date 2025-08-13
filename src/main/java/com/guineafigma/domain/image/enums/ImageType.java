package com.guineafigma.domain.image.enums;

import lombok.Getter;

@Getter
public enum ImageType {
    // 유저 이미지
    USER_DIVING_LOG,
    USER_CHAT,
    USER_LICENSE,
    USER_DIARY,
    
    // 테스트용 이미지 타입
    USER_TEST_POST,
    
    // 시스템 이미지 (모든 유저에게 공통인 것들을 처리하면 됩니다.)
    SYSTEM_DOGAM,           
    SYSTEM_DOGAM_PROFILE;
}