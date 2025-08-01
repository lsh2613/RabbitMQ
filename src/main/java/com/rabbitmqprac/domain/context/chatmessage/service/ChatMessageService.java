package com.rabbitmqprac.domain.context.chatmessage.service;

import com.rabbitmqprac.application.dto.chatmessage.req.ChatMessageReq;
import com.rabbitmqprac.domain.persistence.chatmessage.entity.ChatMessage;
import com.rabbitmqprac.domain.persistence.chatmessage.repository.ChatMessageRepository;
import com.rabbitmqprac.domain.persistence.chatroom.entity.ChatRoom;
import com.rabbitmqprac.domain.persistence.chatroommember.entity.ChatRoomMember;
import com.rabbitmqprac.domain.persistence.chatroommember.repository.ChatRoomMemberRepository;
import com.rabbitmqprac.domain.context.common.service.EntityFacade;
import com.rabbitmqprac.application.dto.chatmessage.res.ChatMessageRes;
import com.rabbitmqprac.application.dto.chatmessage.res.MessageRes;
import com.rabbitmqprac.domain.persistence.user.entity.User;
import com.rabbitmqprac.global.helper.RabbitPublisher;
import com.rabbitmqprac.domain.persistence.chatroom.repository.ChatRoomRedisRepository;
import com.rabbitmqprac.global.helper.StompHeaderAccessorHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final EntityFacade entityFacade;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final RabbitPublisher rabbitPublisher;
    private final ChatRoomRedisRepository chatRoomRedisRepository;
    private final StompHeaderAccessorHelper stompHeaderAccessorHelper;

    @Transactional
    public void sendMessage(StompHeaderAccessor accessor, ChatMessageReq req) {
        Long memberId = stompHeaderAccessorHelper.getUserIdInSession(accessor);
        User user = entityFacade.getUser(memberId);

        Long chatRoomId = stompHeaderAccessorHelper.getChatRoomIdInSession(accessor);
        ChatRoom chatRoom = entityFacade.getChatRoom(chatRoomId);

        ChatMessage chatMessage = saveChatMessage(req, chatRoom, user);

        int unreadCnt = calculateUnreadCntAtPublish(chatRoom.getId());
        sendMessage(user, chatMessage, unreadCnt, chatRoom);
    }

    @Transactional(readOnly = true)
    public List<MessageRes> getChatMessages(Long chatRoomId) {
        ChatRoom chatRoom = entityFacade.getChatRoom(chatRoomId);

        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoom.getId());

        List<MessageRes> messageResList = chatMessages.stream()
                .map(chatMessage -> {
                    /*
                    현재 접속 중인 채팅방 유저를 제외하고 나머지 채팅방 유저의 마지막 입장 시간을 가져옴
                    그 중에서 메시지 생성 시간보다 늦은 시간에 입장한 유저(해당 메시지를 읽지 않았다는 의미)의 수를 세어줌
                    unreadCnt = 채팅방 유저 수 - 현재 접속 중인 유저 수 - 메시지 생성 시간보다 늦은 시간에 입장한 유저 수
                     */
                    int unreadCnt = calculateUnreadCntAtReadTime(chatRoom.getId(), chatMessage.getCreatedAt());
                    User user = entityFacade.getUser(chatMessage.getUserId());
                    return ChatMessageRes.createRes(user.getUsername(), chatMessage, unreadCnt);
                })
                .toList();

        return messageResList;
    }

    private ChatMessage saveChatMessage(ChatMessageReq req, ChatRoom chatRoom, User user) {
        ChatMessage chatMessage = req.createChatMessage(chatRoom.getId(), user.getId());
        chatMessageRepository.save(chatMessage);
        return chatMessage;
    }

    private int calculateUnreadCntAtPublish(Long chatRoomId) {
        int onlineChatRoomMemberCnt = chatRoomRedisRepository.getOnlineChatRoomMemberCnt(chatRoomId);
        int chatRoomMemberCnt = chatRoomMemberRepository.countByChatRoomId(chatRoomId);
        int unreadCnt = chatRoomMemberCnt - onlineChatRoomMemberCnt;
        return unreadCnt;
    }

    private int calculateUnreadCntAtReadTime(Long chatRoomId, LocalDateTime messageCreatedAt) {
        List<ChatRoomMember> chatRoomMembers = chatRoomMemberRepository.findAllByChatRoomId(chatRoomId);
        Set<Long> onlineChatRoomMembers = chatRoomRedisRepository.getOnlineChatRoomMembers(chatRoomId);
        List<LocalDateTime> lastExitAts = getLastExitAtsExcludingOnlineMembers(chatRoomMembers, onlineChatRoomMembers);

        int memberCntAfterMessageCreated = (int) lastExitAts.stream()
                .filter(time -> time.isAfter(messageCreatedAt))
                .count();

        return chatRoomMembers.size() - onlineChatRoomMembers.size() - memberCntAfterMessageCreated;
    }

    private List<LocalDateTime> getLastExitAtsExcludingOnlineMembers(List<ChatRoomMember> chatRoomMembers, Set<Long> onlineMemberIds) {
        return chatRoomMembers.stream()
                .filter(chatRoomMember -> !onlineMemberIds.contains(chatRoomMember.getUser().getId()))
                .map(ChatRoomMember::getLastExitAt)
                .toList();
    }

    private void sendMessage(User user, ChatMessage chatMessage, int unreadCnt, ChatRoom chatRoom) {
        MessageRes messageRes = ChatMessageRes.createRes(user.getUsername(), chatMessage, unreadCnt);
        rabbitPublisher.publish(chatRoom.getId(), messageRes);
    }
}
