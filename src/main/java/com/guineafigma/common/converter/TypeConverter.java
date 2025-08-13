package com.guineafigma.common.converter;

import java.util.HashMap;
import java.util.Map;

// 타입 변환을 위한 간단한 유틸리티 클래스
public class TypeConverter {

    // Object를 HashMap<String, Object>로 변환
    public static HashMap<String, Object> castToHashMap(Object obj) {
        if (obj instanceof HashMap) {
            return createHashMapFromHashMap((HashMap<?, ?>) obj);
        }
        throw new ClassCastException("HashMap<String, Object> 타입이 아닙니다. : " + obj.getClass().getSimpleName());
    }

    // HashMap에서 새로운 HashMap 생성 (타입 체크 기반)
    private static HashMap<String, Object> createHashMapFromHashMap(HashMap<?, ?> source) {
        HashMap<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            
            // 키가 String인지 확인
            if (key instanceof String) {
                result.put((String) key, value);
            } else {
                // 키를 String으로 변환
                result.put(String.valueOf(key), value);
            }
        }
        return result;
    }
} 