package com.guineafigma.domain.logosong.repository;

import com.guineafigma.domain.logosong.entity.LogoSong;
import com.guineafigma.common.enums.MusicGenre;
import com.guineafigma.common.enums.VersionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogoSongRepository extends JpaRepository<LogoSong, Long> {

    Page<LogoSong> findByMusicGenre(MusicGenre musicGenre, Pageable pageable);
    
    Page<LogoSong> findByVersion(VersionType version, Pageable pageable);
    
    Page<LogoSong> findByIndustryContaining(String industry, Pageable pageable);
    
    Page<LogoSong> findByServiceNameContaining(String serviceName, Pageable pageable);
    
    @Query("SELECT l FROM LogoSong l WHERE l.moodTone LIKE %:moodTone%")
    Page<LogoSong> findByMoodToneContaining(@Param("moodTone") String moodTone, Pageable pageable);
    
    @Query("SELECT l FROM LogoSong l ORDER BY l.likeCount DESC")
    Page<LogoSong> findByOrderByLikeCountDesc(Pageable pageable);
    
    @Query("SELECT l FROM LogoSong l ORDER BY l.viewCount DESC")
    Page<LogoSong> findByOrderByViewCountDesc(Pageable pageable);
    
    @Query("SELECT l FROM LogoSong l ORDER BY l.createdAt DESC")
    Page<LogoSong> findByOrderByCreatedAtDesc(Pageable pageable);
    
    // Suno API 관련 메소드들
    Optional<LogoSong> findBySunoTaskId(String sunoTaskId);
    
    @Query("SELECT l FROM LogoSong l WHERE l.musicStatus = :status")
    List<LogoSong> findByMusicStatus(@Param("status") com.guineafigma.common.enums.MusicGenerationStatus status);
}