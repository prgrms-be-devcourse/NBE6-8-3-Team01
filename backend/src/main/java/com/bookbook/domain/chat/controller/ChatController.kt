// 08-30 유효상 CustomOAuth2User를 Kotlin으로 변환되어 프로퍼티 접근으로 변경
package com.bookbook.domain.chat.controller

import com.bookbook.domain.chat.dto.ChatRoomCreateRequest
import com.bookbook.domain.chat.dto.ChatRoomResponse
import com.bookbook.domain.chat.dto.MessageResponse
import com.bookbook.domain.chat.dto.MessageSendRequest
import com.bookbook.domain.chat.service.ChatService
import com.bookbook.global.rsdata.RsData
import com.bookbook.global.security.CustomOAuth2User
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/bookbook/chat")
class ChatController(
    private val chatService: ChatService
) {
    companion object {
        private val log = LoggerFactory.getLogger(ChatController::class.java)
    }

    /**
     * 채팅방 생성 또는 기존 채팅방 반환
     */
    @PostMapping("/rooms")
    fun createChatRoom(
        @RequestBody @Valid request: ChatRoomCreateRequest,
        @AuthenticationPrincipal user: CustomOAuth2User
    ): ResponseEntity<RsData<ChatRoomResponse>> {
        log.info("채팅방 생성 요청 - userId: {}, rentId: {}, lenderId: {}", 
            user.userId, request.rentId, request.lenderId)

        return try {
            val response = chatService.createOrGetChatRoom(request, user.userId)

            log.info("채팅방 생성/조회 성공 - roomId: {}", response.roomId)
            ResponseEntity.ok(RsData("200", "채팅방이 생성되었습니다.", response))
        } catch (e: IllegalArgumentException) {
            log.error("채팅방 생성 실패 - 잘못된 파라미터: {}", e.message)
            ResponseEntity.badRequest()
                .body(RsData("400", "잘못된 요청입니다: ${e.message}", null))
        } catch (e: Exception) {
            log.error("채팅방 생성 실패 - 상세 에러: ", e)
            val errorMessage = e.message ?: "채팅방 생성 중 오류가 발생했습니다."
            ResponseEntity.badRequest()
                .body(RsData("400", errorMessage, null))
        }
    }

    /**
     * 사용자의 채팅방 목록 조회
     */
    @GetMapping("/rooms")
    fun getChatRooms(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal user: CustomOAuth2User
    ): ResponseEntity<RsData<Page<ChatRoomResponse>>> {
        log.info("채팅방 목록 조회 - userId: {}, page: {}, size: {}", user.userId, page, size)

        return try {
            val pageable: Pageable = PageRequest.of(page, size)
            val chatRooms = chatService.getChatRooms(user.userId, pageable)

            ResponseEntity.ok(RsData("200", "채팅방 목록을 조회했습니다.", chatRooms))
        } catch (e: Exception) {
            log.error("채팅방 목록 조회 실패", e)
            ResponseEntity.internalServerError()
                .body(RsData("500", "채팅방 목록 조회에 실패했습니다.", null))
        }
    }

    /**
     * 채팅방 정보 조회
     */
    @GetMapping("/rooms/{roomId}")
    fun getChatRoom(
        @PathVariable roomId: String,
        @AuthenticationPrincipal user: CustomOAuth2User
    ): ResponseEntity<RsData<ChatRoomResponse>> {
        log.info("채팅방 정보 조회 - roomId: {}, userId: {}", roomId, user.userId)

        return try {
            val chatRoom = chatService.getChatRoom(roomId, user.userId)
            ResponseEntity.ok(RsData("200", "채팅방 정보를 조회했습니다.", chatRoom))
        } catch (e: Exception) {
            log.error("채팅방 정보 조회 실패", e)
            when {
                e.message?.contains("권한이 없습니다") == true -> {
                    ResponseEntity.status(403)
                        .body(RsData("403", e.message!!, null))
                }
                else -> {
                    ResponseEntity.badRequest()
                        .body(RsData("400", e.message ?: "채팅방 조회 실패", null))
                }
            }
        }
    }

    /**
     * 채팅방 메시지 목록 조회
     */
    @GetMapping("/rooms/{roomId}/messages")
    fun getChatMessages(
        @PathVariable roomId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @AuthenticationPrincipal user: CustomOAuth2User
    ): ResponseEntity<RsData<Page<MessageResponse>>> {
        log.info("채팅 메시지 조회 - roomId: {}, userId: {}, page: {}, size: {}", 
            roomId, user.userId, page, size)

        return try {
            val pageable: Pageable = PageRequest.of(page, size)
            val messages = chatService.getChatMessages(roomId, user.userId, pageable)

            ResponseEntity.ok(RsData("200", "채팅 메시지를 조회했습니다.", messages))
        } catch (e: Exception) {
            log.error("채팅 메시지 조회 실패", e)
            when {
                e.message?.contains("권한이 없습니다") == true -> {
                    ResponseEntity.status(403)
                        .body(RsData("403", e.message!!, null))
                }
                else -> {
                    ResponseEntity.internalServerError()
                        .body(RsData("500", "채팅 메시지 조회에 실패했습니다.", null))
                }
            }
        }
    }

    /**
     * 메시지 전송
     */
    @PostMapping("/messages")
    fun sendMessage(
        @RequestBody @Valid request: MessageSendRequest,
        @AuthenticationPrincipal user: CustomOAuth2User
    ): ResponseEntity<RsData<MessageResponse>> {
        log.info("메시지 전송 - roomId: {}, userId: {}, messageType: {}", 
            request.roomId, user.userId, request.messageType)

        return try {
            val response = chatService.sendMessage(request, user.userId)

            ResponseEntity.ok(RsData("200", "메시지가 전송되었습니다.", response))
        } catch (e: Exception) {
            log.error("메시지 전송 실패", e)
            when {
                e.message?.contains("권한이 없습니다") == true -> {
                    ResponseEntity.status(403)
                        .body(RsData("403", e.message!!, null))
                }
                else -> {
                    ResponseEntity.badRequest()
                        .body(RsData("400", e.message ?: "메시지 전송 실패", null))
                }
            }
        }
    }

    /**
     * 채팅방 메시지 읽음 처리
     */
    @PatchMapping("/rooms/{roomId}/read")
    fun markMessagesAsRead(
        @PathVariable roomId: String,
        @AuthenticationPrincipal user: CustomOAuth2User
    ): ResponseEntity<RsData<Unit>> {
        log.info("메시지 읽음 처리 - roomId: {}, userId: {}", roomId, user.userId)

        return try {
            chatService.markMessagesAsRead(roomId, user.userId)
            ResponseEntity.ok(RsData("200", "메시지를 읽음 처리했습니다.", Unit))
        } catch (e: Exception) {
            log.error("메시지 읽음 처리 실패", e)
            when {
                e.message?.contains("권한이 없습니다") == true -> {
                    ResponseEntity.status(403)
                        .body(RsData("403", e.message!!, null))
                }
                else -> {
                    ResponseEntity.internalServerError()
                        .body(RsData("500", "메시지 읽음 처리에 실패했습니다.", null))
                }
            }
        }
    }

    /**
     * 사용자의 읽지 않은 메시지 총 개수 조회
     */
    @GetMapping("/unread-count")
    fun getUnreadMessageCount(
        @AuthenticationPrincipal user: CustomOAuth2User
    ): ResponseEntity<RsData<Long>> {
        log.info("읽지 않은 메시지 개수 조회 - userId: {}", user.userId)

        return try {
            val unreadCount = chatService.getUnreadMessageCount(user.userId)
            ResponseEntity.ok(RsData("200", "읽지 않은 메시지 개수를 조회했습니다.", unreadCount))
        } catch (e: Exception) {
            log.error("읽지 않은 메시지 개수 조회 실패", e)
            ResponseEntity.internalServerError()
                .body(RsData("500", "읽지 않은 메시지 개수 조회에 실패했습니다.", null))
        }
    }

    /**
     * 채팅방 나가기
     */
    @PostMapping("/rooms/{roomId}/leave")
    fun leaveChatRoom(
        @PathVariable roomId: String,
        @AuthenticationPrincipal user: CustomOAuth2User
    ): ResponseEntity<RsData<Unit>> {
        log.info("채팅방 나가기 요청 - roomId: {}, userId: {}", roomId, user.userId)

        return try {
            chatService.leaveChatRoom(roomId, user.userId)
            ResponseEntity.ok(RsData("200", "채팅방을 나갔습니다.", Unit))
        } catch (e: Exception) {
            log.error("채팅방 나가기 실패", e)
            when {
                e.message?.contains("권한이 없습니다") == true -> {
                    ResponseEntity.status(403)
                        .body(RsData("403", e.message!!, null))
                }
                e.message?.contains("존재하지 않습니다") == true -> {
                    ResponseEntity.status(404)
                        .body(RsData("404", e.message!!, null))
                }
                else -> {
                    ResponseEntity.internalServerError()
                        .body(RsData("500", "채팅방 나가기에 실패했습니다.", null))
                }
            }
        }
    }
}