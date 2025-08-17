package com.guineafigma.domain.image.repository;

import com.guineafigma.domain.image.entity.Image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    
    // 사용자 ID로 이미지 목록 조회
    List<Image> findByUserId(Long userId);
        
    // S3 키로 이미지 조회
    Optional<Image> findByS3Key(String s3Key);
    
    // S3 키가 특정 패턴으로 시작하는 이미지 목록 조회
    List<Image> findByS3KeyStartingWith(String s3KeyPrefix);
    
    
    
    // postId가 null인 temp 이미지들 조회
    List<Image> findByPostIdIsNull();
    
    // 24시간이 지난 temp 경로 고아 이미지들 조회 (postId가 null이고 생성일이 기준 시간 이전, temp 경로 포함)
    @Query("SELECT i FROM Image i WHERE i.postId IS NULL AND i.createdAt < :cutoffTime AND i.s3Key LIKE '%/temp/%'")
    List<Image> findOrphanedTempImages(@Param("cutoffTime") LocalDateTime cutoffTime);
} 