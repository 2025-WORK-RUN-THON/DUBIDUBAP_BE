package com.guineafigma.domain.logbase.logdiary.service;

import com.guineafigma.domain.image.service.ImageService;
import com.guineafigma.domain.logbase.LogBaseInfo;
import com.guineafigma.domain.logbase.LogBaseInfoService;
import com.guineafigma.domain.logbase.logdiary.dto.DiaryRequest;
import com.guineafigma.domain.logbase.logdiary.dto.DiaryResponse;
import com.guineafigma.domain.logbase.logdiary.entity.Diary;
import com.guineafigma.domain.logbase.logdiary.repository.DiaryRepository;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.guineafigma.domain.image.enums.ImageType;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final LogBaseInfoService logBaseInfoService;
    private final ObjectMapper objectMapper;
    private final ImageService imageService;

    @Transactional
    public DiaryResponse createDiary(Long userId, Long logBaseInfoId, DiaryRequest request) {
        // 로그베이스의 존재와 접근 권한 확인
        LogBaseInfo logBaseInfo = logBaseInfoService.validateAccess(logBaseInfoId, userId);

        // 이후 다이어리 중복 여부 확인 JSON
        if (diaryRepository.existsByLogBaseInfoId(logBaseInfoId)) {
            throw new BusinessException(ErrorCode.DIARY_ALREADY_EXISTS);
        }

        // 이미지 임시 url이 포함된 contents을 JSON 문자열로 변환
        String contentJson = toJson(request.getContents());

        // ImageService를 호출하여 이미지 처리 및 영구 URL로 교체된 JSON 받기
        String processedContentJson = imageService.processContentAndUpdateImages(
                contentJson,
                ImageType.USER_DIARY,
                userId,
                logBaseInfoId,
                null                  // 이전 콘텐츠 (생성이므로 null)
        );
        Diary diary = Diary.builder()
                .logBaseInfo(logBaseInfo)
                .contentJson(processedContentJson)
                .build();

        diaryRepository.save(diary);
        return DiaryResponse.from(diary, processedContentJson, objectMapper);
    }

    @Transactional
    public DiaryResponse updateDiary(Long userId, Long logBaseInfoId, DiaryRequest request) {
        Diary diary = getDiaryWithAuth(logBaseInfoId, userId);
        // DB에 저장된 기존 콘텐츠 (삭제된 이미지를 찾기 위함)
        String previousContentJson = diary.getContentJson();

        // 요청받은 새 컨텐츠를 JSON 문자열로 변환
        String newContentJson = toJson(request.getContents());

        // ImageService를 호출하여 이미지 추가/삭제 처리 및 최종 JSON 받기
        String processedContentJson = imageService.processContentAndUpdateImages(
                newContentJson,
                ImageType.USER_DIARY,
                userId,
                logBaseInfoId,
                previousContentJson     // 이전 콘텐츠
        );

        diary.updateContent(processedContentJson);
        return DiaryResponse.from(diary, processedContentJson, objectMapper);

    }

    @Transactional(readOnly = true)
    public DiaryResponse getDiary(Long userId, Long logBaseInfoId) {
        Diary diary = getDiaryWithAuth(logBaseInfoId, userId);
        return DiaryResponse.from(diary, diary.getContentJson(), objectMapper);
    }

    private String toJson(Object contents) {
        try {
            return objectMapper.writeValueAsString(contents);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_JSON_FORMAT);
        }
    }


    private Diary getDiaryWithAuth(Long logBaseInfoId, Long userId) {
        // 다이어리를 update 하거나 get 할때, 로그북 베이스에 일기가 존재하는지 확인
        Diary diary = diaryRepository.findByLogBaseInfoId(logBaseInfoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        // 다이어리를 update 하거나 get 할때, 일기에 접근 권한이 있는지 확인
        if (!diary.getLogBaseInfo().getMember().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_FORBIDDEN_ACCESS);
        }

        return diary;
    }

}