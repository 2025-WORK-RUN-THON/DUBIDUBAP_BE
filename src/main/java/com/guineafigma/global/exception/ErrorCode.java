package com.guineafigma.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 실제 사용되는 공통 에러들
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_002", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_003", "서버 내부 오류가 발생했습니다."),

    // 실제 사용되는 검증 에러들
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_001", "입력값 검증에 실패했습니다."),
    REQUIRED_FIELD_MISSING(HttpStatus.BAD_REQUEST, "VALIDATION_002", "필수 필드가 누락되었습니다."),

    // 해양도감 관련 에러코드
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "ENCYCLOPEDIA_001", "해당 카드에 대한 정보를 찾을 수 없습니다."),
    TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "ENCYCLOPEDIA_002", "존재하지 않는 종류입니다."),

    // 다이어리 관련 에러코드
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "DIARY_001", "해당 로그의 다이어리를 찾을 수 없습니다."),
    DIARY_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "DIARY_002", "해당 로그는 이미 다이어리가 존재합니다."),
    INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "DIARY_003", "다이어리 콘텐츠의 JSON 구조가 잘못되었습니다."),
    DIARY_FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "DIARY_004", "다이어리에 접근 권한이 없습니다."),

    //로그북 관련 에러코드
    LOG_BASE_NOT_FOUND(HttpStatus.NOT_FOUND, "LOGBOOK_001", "해당 날짜에는 로그북을 찾을 수 없습니다."),
    LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "LOGBOOK_002", "해당 로그북의 세부 정보를 찾을 수 없습니다."),
    LOG_LIMIT_EXCEEDED(HttpStatus.NOT_FOUND, "LOGBOOK_003", "로그북은 하루에 최대 3개까지만 생성할 수 있습니다."),
    LOG_ACCESS_DENIED(HttpStatus.FORBIDDEN,"LOGBOOK_004","로그북에 접근 권한이 없습니다."),

    //로그베이스 관련 에러코드
    LOG_BASE_FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "LOGBASE_001", "로그 베이스에 접근 권한이 없습니다."),

    //맴버 관련
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_001", "이메일을 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_002", "유저를 찾을 수 없습니다."),

    //소셜 로그인 관련
    GOOGLE_BAD_GATEWAY(HttpStatus.BAD_GATEWAY, "GOOGLE_001", "구글 유저를 찾을 수 없습니다"),

    //알림 관련
    NOTIFICAITION_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_003", "해당 ID를 가진 사용자의 알림이 존재하지 않습니다"),

    AVATAR_NOT_FOUND(HttpStatus.NOT_FOUND, "AVATAR_001", "해당 유저의 아바타를 찾을 수 업습니다."),

    // 채팅방 관련 에러코드
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_001", "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHAT_ROOM_002", "채팅방에 접근 권한이 없습니다."),
    CHAT_ROOM_MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "CHAT_ROOM_003", "메시지가 너무 깁니다."),
    
    // OpenAI API 관련 에러코드
    OPENAI_API_ERROR(HttpStatus.BAD_GATEWAY, "OPENAI_001", "AI 서비스에 일시적인 문제가 발생했습니다."),
    OPENAI_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "OPENAI_002", "AI 서비스 사용량이 초과되었습니다."),
    OPENAI_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "OPENAI_003", "AI 서비스 요청이 올바르지 않습니다."),
    OPENAI_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "OPENAI_004", "AI 서비스 응답 시간이 초과되었습니다."),
    
    // 이미지 처리 관련 에러코드
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_001", "이미지 업로드에 실패했습니다."),
    IMAGE_SIZE_TOO_LARGE(HttpStatus.BAD_REQUEST, "IMAGE_002", "이미지 크기와 용량이 너무 큽니다."),
    IMAGE_FORMAT_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "IMAGE_003", "지원하지 않는 이미지 형식입니다."),
    IMAGE_URL_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "IMAGE_004", "올바르지 않은 이미지 URL 형식입니다."),
    
    // 인증 관련 에러코드 강화
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "토큰이 유효하지 않습니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_002", "액세스 토큰이 만료되었습니다."), // TODO: 토큰 만료 시 401 에러 처리 필요
    INVALID_USER_CONTEXT(HttpStatus.UNAUTHORIZED, "AUTH_003", "사용자 인증 정보가 유효하지 않습니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_004", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_005", "접근이 거절되었습니다.");

    // TODO: 비즈니스 로직 개발하면서 필요한 에러코드들 추가
    private final HttpStatus status;
    private final String code;
    private final String message;
}