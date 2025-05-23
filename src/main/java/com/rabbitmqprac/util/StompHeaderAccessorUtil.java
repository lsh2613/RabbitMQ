package com.rabbitmqprac.util;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StompHeaderAccessorUtil {
    static final String CHAT_ROOM_ID = "chat-room-id";
    static final String MEMBER_ID = "member-id";

    public void setMemberIdInSession(StompHeaderAccessor accessor, Long memberId) {
        accessor.getSessionAttributes().put(MEMBER_ID, memberId);
    }

    public Long getMemberIdInSession(StompHeaderAccessor accessor) {
        return Optional.ofNullable((Long) accessor.getSessionAttributes().get(MEMBER_ID))
                .orElseThrow(() -> new RuntimeException("Stomp header session에 memberId가 존재하지 않습니다"));
    }

    public Long removeMemberIdInSession(StompHeaderAccessor accessor) {
        return Optional.ofNullable((Long) accessor.getSessionAttributes().remove(MEMBER_ID))
                .orElseThrow(() -> new RuntimeException("Stomp header session에 memberId가 존재하지 않습니다"));
    }

    public Long getChatRoomIdInHeader(StompHeaderAccessor accessor) {
        return Optional.ofNullable(accessor.getFirstNativeHeader(CHAT_ROOM_ID))
                .map(Long::valueOf)
                .orElseThrow(() -> new RuntimeException("Stomp header에 chat-room-id 존재하지 않습니다"));
    }

    public void setChatRoomIdInSession(StompHeaderAccessor accessor, Long chatRoomId) {
        accessor.getSessionAttributes().put(CHAT_ROOM_ID, chatRoomId);
    }

    public Long getChatRoomIdInSession(StompHeaderAccessor accessor) {
        return Optional.ofNullable((Long) accessor.getSessionAttributes().get(CHAT_ROOM_ID))
                .orElseThrow(() -> new RuntimeException("Stomp header session에 chat-room-id가 존재하지 않습니다"));
    }

    public Long removeChatRoomIdInSession(StompHeaderAccessor accessor) {
        return Optional.ofNullable((Long) accessor.getSessionAttributes().remove(CHAT_ROOM_ID))
                .orElseThrow(() -> new RuntimeException("Stomp header session에 chat-room-id가 존재하지 않습니다"));
    }
}
