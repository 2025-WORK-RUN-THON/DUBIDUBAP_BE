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

    // 사용자 관련 에러코드
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    USER_NOT_ACTIVE(HttpStatus.UNAUTHORIZED, "USER_002", "비활성화된 사용자입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "USER_003", "이미 사용 중인 닉네임입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_004", "비밀번호가 일치하지 않습니다. 다른 닉네임으로 가입해 주세요."),
    NICKNAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "USER_005", "닉네임은 2자 이상이어야 합니다."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "USER_006", "닉네임은 20자 이하여야 합니다."),
    PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "USER_007", "비밀번호는 4자 이상이어야 합니다."),

    // 로고송 관련 에러코드
    LOGOSONG_NOT_FOUND(HttpStatus.NOT_FOUND, "LOGOSONG_001", "로고송을 찾을 수 없습니다."),
    LOGOSONG_ACCESS_DENIED(HttpStatus.FORBIDDEN, "LOGOSONG_002", "로고송에 접근 권한이 없습니다."),
    LOGOSONG_LIKE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "LOGOSONG_003", "이미 좋아요를 누른 로고송입니다."),
    LOGOSONG_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "LOGOSONG_004", "좋아요 정보를 찾을 수 없습니다."),
    INVALID_MUSIC_GENRE(HttpStatus.BAD_REQUEST, "LOGOSONG_005", "유효하지 않은 음악 장르입니다."),
    INVALID_VERSION_TYPE(HttpStatus.BAD_REQUEST, "LOGOSONG_006", "유효하지 않은 버전 타입입니다."),
    SERVICE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "LOGOSONG_007", "서비스명은 필수입니다."),

    // 미디어 관련 에러코드
    MEDIA_NOT_FOUND(HttpStatus.NOT_FOUND, "MEDIA_001", "미디어 파일을 찾을 수 없습니다."),
    MEDIA_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_002", "미디어 파일 업로드에 실패했습니다."),
    MEDIA_SIZE_TOO_LARGE(HttpStatus.BAD_REQUEST, "MEDIA_003", "미디어 파일 크기가 너무 큽니다."),
    MEDIA_FORMAT_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "MEDIA_004", "지원하지 않는 미디어 형식입니다."),
    MEDIA_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MEDIA_005", "미디어 파일에 접근 권한이 없습니다."),
    
    // 외부 API 관련 에러코드 (필요시 사용)
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "API_001", "외부 서비스에 일시적인 문제가 발생했습니다."),
    EXTERNAL_API_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "API_002", "외부 서비스 응답 시간이 초과되었습니다."),
    
    // 인증 관련 에러코드 강화
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "토큰이 유효하지 않습니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_002", "액세스 토큰이 만료되었습니다."),
    INVALID_USER_CONTEXT(HttpStatus.UNAUTHORIZED, "AUTH_003", "사용자 인증 정보가 유효하지 않습니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_004", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_005", "접근이 거절되었습니다."),

    // Suno API 관련 에러코드
    SUNO_API_ERROR(HttpStatus.BAD_GATEWAY, "SUNO_001", "Suno API 호출 실패"),
    SUNO_API_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "SUNO_002", "Suno API 인증 실패"),
    SUNO_API_BAD_REQUEST(HttpStatus.BAD_REQUEST, "SUNO_003", "Suno API 요청 데이터 오류"),
    SUNO_TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "SUNO_004", "Suno 작업을 찾을 수 없습니다."),
    MUSIC_GENERATION_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "SUNO_005", "음악 생성 시간 초과"),
    MUSIC_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SUNO_006", "음악 생성 실패"),
    MUSIC_GENERATION_IN_PROGRESS(HttpStatus.CONFLICT, "SUNO_007", "이미 음악 생성이 진행 중입니다."),
    LYRICS_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SUNO_008", "가사 생성 실패");
    private final HttpStatus status;
    private final String code;
    private final String message;
}