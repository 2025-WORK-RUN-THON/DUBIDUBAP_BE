package com.guineafigma.domain.media.entity;

import com.guineafigma.common.entity.BaseEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "미디어 엔티티")
public class Media extends BaseEntity {

    @Column(name = "s3_key", nullable = false, length = 255)
    @Schema(description = "S3 저장 키")
    private String s3Key;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "file_type", length = 50)
    @Schema(description = "파일 타입 (image, audio)")
    private String fileType;

    @Column(name = "width")
    private Long width;

    @Column(name = "height")
    private Long height;

    @Column(name = "duration")
    @Schema(description = "오디오 파일 길이 (초)")
    private Long duration;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "logosong_id")
    @Schema(description = "로고송 ID (FK 없이 참조)")
    private Long logosongId;

    @Builder
    public Media(String s3Key, String originalFilename, String fileType, Long width, Long height, Long duration, Long userId, Long logosongId) {
        this.s3Key = s3Key;
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.userId = userId;
        this.logosongId = logosongId;
    }

    public void updateS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public void updateLogosongId(Long logosongId) {
        this.logosongId = logosongId;
    }

    public void updateFileType(String fileType) {
        this.fileType = fileType;
    }
}


