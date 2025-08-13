package com.guineafigma.common.util;

import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;

import java.util.Arrays;

public class EnumValidator {

    // enum 반환
    public static <E extends Enum<E>> E validateEnum(Class<E> enumClass, String value) {
        String upperValue = value.toUpperCase();
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.name().equals(upperValue))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        String.format("'%s'는 %s enum에 존재하지 않는 값입니다.", value, enumClass.getSimpleName())));
    }

    //반환 x 유효성만 검사
    public static <E extends Enum<E>> void validateEnumOrThrow(Class<E> enumClass, String value) {
        String upperValue = value.toUpperCase();
        if (Arrays.stream(enumClass.getEnumConstants()).noneMatch(e -> e.name().equals(upperValue))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    String.format("'%s'는 %s enum에 존재하지 않는 값입니다.", value, enumClass.getSimpleName()));
        }
    }

    //Boolean 반환
    public static <E extends Enum<E>> boolean isValidEnum(Class<E> enumClass, String value) {
        String upperValue = value.toUpperCase();
        return Arrays.stream(enumClass.getEnumConstants())
                .anyMatch(e -> e.name().equals(upperValue));
    }
}
