package com.guineafigma.domain.logosong.repository;

import com.guineafigma.domain.logosong.entity.LogoSongLike;
import com.guineafigma.domain.logosong.entity.LogoSongLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogoSongLikeRepository extends JpaRepository<LogoSongLike, LogoSongLikeId> {

    Optional<LogoSongLike> findByUserIdAndLogosongId(Long userId, Long logosongId);
    
    List<LogoSongLike> findByUserId(Long userId);
    
    List<LogoSongLike> findByLogosongId(Long logosongId);
    
    @Query("SELECT COUNT(l) FROM LogoSongLike l WHERE l.logosongId = :logosongId")
    Long countByLogosongId(@Param("logosongId") Long logosongId);
    
    boolean existsByUserIdAndLogosongId(Long userId, Long logosongId);
}