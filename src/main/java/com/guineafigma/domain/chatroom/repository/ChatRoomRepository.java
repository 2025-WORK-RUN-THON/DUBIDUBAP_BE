package com.guineafigma.domain.chatroom.repository;

import com.guineafigma.domain.chatroom.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 사용자 ID로 채팅방 목록 조회
    // TODO : 추후 채팅방 조회 시 정렬 순서 파라미터 추가 필요
    List<ChatRoom> findByUserIdOrderByUpdatedAtDesc(Long userId);

    // 채팅방 ID와 사용자 ID로 채팅방 조회 (소유권 검증 포함)
    Optional<ChatRoom> findByIdAndUserId(Long id, Long userId);
}
