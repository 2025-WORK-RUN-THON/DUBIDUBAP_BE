package com.guineafigma.domain.media.repository;

import com.guineafigma.domain.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByUserId(Long userId);
    Optional<Media> findByS3Key(String s3Key);
    List<Media> findByS3KeyStartingWith(String s3KeyPrefix);
    List<Media> findByPostIdIsNull();
    List<Media> findAllByCreatedAtBeforeAndS3KeyContaining(LocalDateTime cutoffTime, String contains);
}


