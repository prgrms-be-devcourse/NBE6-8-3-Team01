// 08-30 CustomOAuth2User를 Kotlin으로 변환되어 프로퍼티 접근으로 변경
package com.bookbook.domain.chat.controller

import com.bookbook.domain.chat.dto.MessageSendRequest
import com.bookbook.domain.chat.service.ChatService
import com.bookbook.global.security.CustomOAuth2User
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller

@Controller
class WebSocketChatController(
    private val messagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService
) {
    companion object {
        private val log = LoggerFactory.getLogger(WebSocketChatController::class.java)
    }

    /**
     * 채팅 메시지 전송 (WebSocket)
     */
    @MessageMapping("/chat.sendMessage")
    fun sendMessage(@Payload request: MessageSendRequest, headerAccessor: SimpMessageHeaderAccessor) {
        try {
            // 인증 정보에서 사용자 ID 추출
            val auth = headerAccessor.user as? Authentication
            if (auth?.principal !is CustomOAuth2User) {
                log.error("WebSocket 메시지 전송 실패 - 인증되지 않은 사용자")
                return
            }

            val user = auth.principal as CustomOAuth2User
            val userId = user.userId

            log.info("WebSocket 메시지 전송 - roomId: {}, userId: {}", request.roomId, userId)

            // 메시지 저장
            val messageResponse = chatService.sendMessage(request, userId)

            // 채팅방 구독자들에게 메시지 브로드캐스트
            messagingTemplate.convertAndSend("/topic/chat/${request.roomId}", messageResponse)

            log.info("WebSocket 메시지 전송 완료 - messageId: {}", messageResponse.id)
        } catch (e: Exception) {
            log.error("WebSocket 메시지 전송 실패", e)
        }
    }

    /**
     * 채팅방 입장 (WebSocket)
     */
    @MessageMapping("/chat.addUser")
    fun addUser(@Payload roomId: String, headerAccessor: SimpMessageHeaderAccessor) {
        try {
            // 인증 정보에서 사용자 정보 추출
            val auth = headerAccessor.user as? Authentication
            if (auth?.principal !is CustomOAuth2User) {
                log.error("WebSocket 사용자 입장 실패 - 인증되지 않은 사용자")
                return
            }

            val user = auth.principal as CustomOAuth2User

            log.info("WebSocket 사용자 입장 - roomId: {}, userId: {}, nickname: {}", 
                roomId, user.userId, user.nickname)

            // 세션에 사용자 정보 저장
            headerAccessor.sessionAttributes?.apply {
                put("userId", user.userId)
                put("roomId", roomId)
                put("nickname", user.nickname)
            }
        } catch (e: Exception) {
            log.error("WebSocket 사용자 입장 실패", e)
        }
    }

    /**
     * 메시지 읽음 처리 (WebSocket)
     */
    @MessageMapping("/chat.markAsRead")
    fun markAsRead(@Payload roomId: String, headerAccessor: SimpMessageHeaderAccessor) {
        try {
            // 인증 정보에서 사용자 ID 추출
            val auth = headerAccessor.user as? Authentication
            if (auth?.principal !is CustomOAuth2User) {
                log.error("WebSocket 읽음 처리 실패 - 인증되지 않은 사용자")
                return
            }

            val user = auth.principal as CustomOAuth2User
            val userId = user.userId

            log.info("WebSocket 읽음 처리 - roomId: {}, userId: {}", roomId, userId)

            // 메시지 읽음 처리
            chatService.markMessagesAsRead(roomId, userId)

            // 읽음 처리 알림을 채팅방에 브로드캐스트 (선택사항)
            messagingTemplate.convertAndSend(
                "/topic/chat/$roomId/read",
                "${user.nickname}이(가) 메시지를 읽었습니다."
            )
        } catch (e: Exception) {
            log.error("WebSocket 읽음 처리 실패", e)
        }
    }
}