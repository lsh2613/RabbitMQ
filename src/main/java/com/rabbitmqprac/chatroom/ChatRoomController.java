package com.rabbitmqprac.chatroom;

import com.rabbitmqprac.chatroom.dto.ChatRoomCreateReq;
import com.rabbitmqprac.chatroom.dto.ChatRoomCreateRes;
import com.rabbitmqprac.chatroom.dto.ChatRoomRes;
import com.rabbitmqprac.chatroom.dto.MyChatRoomRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/chat-rooms")
    public ChatRoomCreateRes createChatRoom(@RequestBody ChatRoomCreateReq chatRoomCreateReq) {
        return chatRoomService.createChatRoom(chatRoomCreateReq);
    }

    @GetMapping("/chat-rooms")
    public List<MyChatRoomRes> getMyChatRooms(@RequestParam Long loginId) {
        return chatRoomService.getMyChatRooms(loginId);
    }

    @GetMapping("/chat-rooms/all")
    public List<ChatRoomRes> getChatRooms() {
        return chatRoomService.getChatRooms();
    }
}