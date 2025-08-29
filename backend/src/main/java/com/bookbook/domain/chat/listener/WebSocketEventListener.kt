package com.bookbook.domain.chat.listener

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Component
class WebSocketEventListener(
    // 향후 브로드캐스트 기능 사용을 위해 유지
    @Suppress("unused")
    private val messagingTemplate: SimpMessageSendingOperations
) {
    companion object {
        private val log = LoggerFactory.getLogger(WebSocketEventListener::class.java)
    }

    @EventListener
    fun handleWebSocketConnectListener(@Suppress("UNUSED_PARAMETER") event: SessionConnectedEvent) {
        log.info("새로운 WebSocket 연결이 수신되었습니다")
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)

        val sessionAttributes = headerAccessor.sessionAttributes
        val roomId = sessionAttributes?.get("roomId") as? String
        val nickname = sessionAttributes?.get("nickname") as? String
        val userId = sessionAttributes?.get("userId") as? Long

        if (roomId != null && nickname != null) {
            log.info(
                "사용자 연결 해제 - roomId: {}, userId: {}, nickname: {}",
                roomId,
                userId,
                nickname
            )

            // 선택사항: 사용자 나감 알림을 채팅방에 브로드캐스트
            // messagingTemplate.convertAndSend("/topic/chat/$roomId", 
            //         "${nickname}님이 채팅방을 나갔습니다.")
        }
    }
}