package com.guineafigma.domain.user.repository;

import com.guineafigma.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNickname(String nickname);
    
    Optional<User> findByNicknameAndIsActive(String nickname, Boolean isActive);
    
    boolean existsByNickname(String nickname);
}
