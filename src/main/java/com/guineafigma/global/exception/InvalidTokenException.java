package com.guineafigma.global.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);  // 예외 메시지 설정
    }
}