// TODO: MessageType enum을 Kotlin으로 변환 후 import 수정
// TODO: User, Rent entity들이 Kotlin으로 변환되면 프로퍼티 접근으로 변경
package com.bookbook.domain.chat.service

import com.bookbook.domain.chat.dto.ChatRoomCreateRequest
import com.bookbook.domain.chat.dto.ChatRoomResponse
import com.bookbook.domain.chat.dto.MessageResponse
import com.bookbook.domain.chat.dto.MessageSendRequest
import com.bookbook.domain.chat.entity.ChatMessage
import com.bookbook.domain.chat.entity.ChatRoom
import com.bookbook.domain.chat.enums.MessageType
import com.bookbook.domain.chat.repository.ChatMessageRepository
import com.bookbook.domain.chat.repository.ChatRoomRepository
import com.bookbook.domain.rent.entity.Rent
import com.bookbook.domain.rent.repository.RentRepository
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional(readOnly = true)
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository,
    private val rentRepository: RentRepository
) {
    companion object {
        private val log = LoggerFactory.getLogger(ChatService::class.java)
    }

    @Transactional
    fun createOrGetChatRoom(request: ChatRoomCreateRequest, borrowerId: Long): ChatRoomResponse {
        log.info("채팅방 생성 요청 - rentId: {}, lenderId: {}, borrowerId: {}", 
            request.rentId, request.lenderId, borrowerId)

        try {
            val rent = rentRepository.findById(request.rentId)
                .orElseThrow { 
                    log.error("존재하지 않는 대여 게시글 - rentId: {}", request.rentId)
                    ServiceException("존재하지 않는 대여 게시글입니다.")
                }

            val lender = userRepository.findById(request.lenderId)
                .orElseThrow { 
                    log.error("존재하지 않는 빌려주는 사용자 - lenderId: {}", request.lenderId)
                    ServiceException("존재하지 않는 빌려주는 사용자입니다.")
                }

            val borrower = userRepository.findById(borrowerId)
                .orElseThrow { 
                    log.error("존재하지 않는 빌리는 사용자 - borrowerId: {}", borrowerId)
                    ServiceException("존재하지 않는 빌리는 사용자입니다.")
                }

            if (request.lenderId == borrowerId) {
                log.error("자기 자신과의 채팅 시도 - userId: {}", borrowerId)
                throw ServiceException("자기 자신과는 채팅할 수 없습니다.")
            }

            // 기존 채팅방 조회 - 활성 채팅방만 조회
            val existingRooms = chatRoomRepository
                .findByRentIdAndLenderIdAndBorrowerIdOrderByCreatedDateDesc(
                    request.rentId, request.lenderId, borrowerId
                )

            var existingRoom: ChatRoom? = null

            // 활성 채팅방 중에서 사용 가능한 채팅방 찾기
            for (room in existingRooms) {
                if (!room.isActive) {
                    continue  // 비활성 채팅방은 스킵
                }

                val lenderLeft = room.hasUserLeft(request.lenderId)
                val borrowerLeft = room.hasUserLeft(borrowerId)

                // 채팅방 사용 가능 여부 확인 및 처리
                when {
                    // 우선순위 1: 둘 다 활성 상태인 채팅방
                    !lenderLeft && !borrowerLeft -> {
                        existingRoom = room
                        break
                    }
                    // 우선순위 2: 한 명 이상 나간 상태이지만 복구 가능한 채팅방 (기존 활성방이 없을 때만)
                    existingRoom == null -> {
                        log.info("나간 사용자 재참여 처리 - roomId: {}, lenderLeft: {}, borrowerLeft: {}", 
                            room.roomId, lenderLeft, borrowerLeft)

                        // 나간 상태 해제
                        if (lenderLeft) {
                            room.rejoinUser(request.lenderId)
                        }
                        if (borrowerLeft) {
                            room.rejoinUser(borrowerId)
                        }

                        chatRoomRepository.save(room)
                        existingRoom = room
                        break
                    }
                }
            }

            // 중복된 오래된 채팅방들 정리
            if (existingRooms.size > 1) {
                log.warn("중복 채팅방 발견 및 정리 시작 - 개수: {}, rentId: {}, lenderId: {}, borrowerId: {}", 
                    existingRooms.size, request.rentId, request.lenderId, borrowerId)
                cleanupDuplicateChatRooms(existingRooms.toMutableList(), existingRoom)
            }

            existingRoom?.let {
                log.info("기존 활성 채팅방 발견 및 반환 - roomId: {}", it.roomId)
                return buildChatRoomResponse(it, rent, lender, borrower, borrowerId)
            }

            // 새 채팅방 생성
            log.info("새 채팅방 생성 시작")

            val newRoomId = UUID.randomUUID().toString()

            // 최종 중복 체크 (동시성 문제 방지)
            val finalCheck = chatRoomRepository
                .findByRentIdAndLenderIdAndBorrowerIdOrderByCreatedDateDesc(
                    request.rentId, request.lenderId, borrowerId
                )

            if (finalCheck.isNotEmpty()) {
                log.info("채팅방 생성 중 기존 채팅방 발견 - 기존 채팅방 사용: {}", finalCheck[0].roomId)
                return buildChatRoomResponse(finalCheck[0], rent, lender, borrower, borrowerId)
            }

            val newRoom = ChatRoom().apply {
                roomId = newRoomId
                rentId = request.rentId
                lenderId = request.lenderId
                this.borrowerId = borrowerId
                isActive = true
                createdDate = LocalDateTime.now()
            }

            val savedRoom = chatRoomRepository.save(newRoom)
            log.info("새 채팅방 생성 완료 - roomId: {}", savedRoom.roomId)

            return buildChatRoomResponse(savedRoom, rent, lender, borrower, borrowerId)
        } catch (e: ServiceException) {
            log.error("채팅방 생성 실패 (ServiceException) - {}", e.message)
            throw e
        } catch (e: Exception) {
            log.error("채팅방 생성 중 예상치 못한 오류 발생", e)
            throw ServiceException("채팅방 생성 중 오류가 발생했습니다: ${e.message}")
        }
    }

    fun getChatRooms(userId: Long, pageable: Pageable): Page<ChatRoomResponse> {
        log.info("채팅방 목록 조회 - userId: {}", userId)

        val chatRooms = chatRoomRepository.findByUserIdOrderByLastMessageTimeDesc(userId, pageable)

        // 나가지 않은 채팅방만 필터링하여 리스트로 변환
        val validChatRooms = chatRooms.content
            .filter { !it.hasUserLeft(userId) }  // 나가지 않은 채팅방만
            .mapNotNull { room ->
                val (bookTitle, bookImage) = getRentInfo(room.rentId)

                val otherUserId = room.getOtherUserId(userId)
                val otherUser = otherUserId?.let { userRepository.findById(it).orElse(null) }
                val otherUserNickname = otherUser?.nickname ?: "알 수 없는 사용자"

                val unreadCount = chatMessageRepository.countUnreadMessagesByRoomIdAndUserId(room.roomId, userId)

                ChatRoomResponse.from(
                    room, bookTitle, bookImage,
                    otherUserNickname, null, unreadCount
                ).copy(otherUserId = otherUserId)
            }

        // 새로운 Page 객체 생성 (실제 결과 개수로 totalElements 조정)
        return PageImpl(validChatRooms, pageable, validChatRooms.size.toLong())
    }

    fun getChatRoom(roomId: String, userId: Long): ChatRoomResponse {
        log.info("채팅방 정보 조회 - roomId: {}, userId: {}", roomId, userId)

        val chatRoom = chatRoomRepository.findByRoomIdAndUserId(roomId, userId)
            .orElseThrow { ServiceException("채팅방에 접근할 권한이 없습니다.") }

        // 사용자가 나간 상태인 경우 재참여 처리
        if (chatRoom.hasUserLeft(userId)) {
            log.info("나간 사용자 재참여 처리 - roomId: {}, userId: {}", roomId, userId)
            chatRoom.rejoinUser(userId)
            chatRoomRepository.save(chatRoom)
        }

        val (bookTitle, bookImage) = getRentInfo(chatRoom.rentId)

        val otherUserId = chatRoom.getOtherUserId(userId)
        val otherUser = otherUserId?.let { userRepository.findById(it).orElse(null) }
        val otherUserNickname = otherUser?.nickname ?: "알 수 없는 사용자"

        val unreadCount = chatMessageRepository.countUnreadMessagesByRoomIdAndUserId(roomId, userId)

        return ChatRoomResponse.from(
            chatRoom, bookTitle, bookImage,
            otherUserNickname, null, unreadCount
        ).copy(
            otherUserId = otherUserId,
            rentId = chatRoom.rentId
        )
    }

    fun getChatMessages(roomId: String, userId: Long, pageable: Pageable): Page<MessageResponse> {
        log.info("채팅 메시지 조회 - roomId: {}, userId: {}, page: {}, size: {}", 
            roomId, userId, pageable.pageNumber, pageable.pageSize)

        val chatRoom = chatRoomRepository.findByRoomIdAndUserId(roomId, userId)
            .orElseThrow { ServiceException("채팅방에 접근할 권한이 없습니다.") }

        // 사용자가 나간 시간 확인
        val userLeftTime = chatRoom.getUserLeftTime(userId)

        val messages = if (userLeftTime != null) {
            // 나간 시간 이후의 메시지만 조회 (나간 후 새로 온 메시지만)
            log.info("사용자가 나간 시간 이후 메시지만 조회 - userId: {}, leftTime: {}", userId, userLeftTime)
            chatMessageRepository.findByRoomIdAndCreatedDateAfterOrderByCreatedDateDesc(
                roomId, userLeftTime, pageable
            )
        } else {
            // 모든 메시지 조회
            chatMessageRepository.findByRoomIdOrderByCreatedDateDesc(roomId, pageable)
        }

        return messages.map { message ->
            // 시스템 메시지 처리 (senderId가 0인 경우)
            if (message.senderId == 0L) {
                return@map MessageResponse.from(message, "시스템", null, false)
            }

            val sender = userRepository.findById(message.senderId).orElse(null)
            val senderNickname = sender?.nickname ?: "알 수 없는 사용자"

            // isMine 계산: 현재 사용자의 ID와 메시지 발신자 ID가 같은지 확인
            val isMine = message.senderId == userId
            MessageResponse.from(message, senderNickname, null, isMine)
        }
    }

    @Transactional
    fun sendMessage(request: MessageSendRequest, senderId: Long): MessageResponse {
        log.info("메시지 전송 - roomId: {}, senderId: {}", request.roomId, senderId)

        val chatRoom = chatRoomRepository.findByRoomIdAndUserId(request.roomId, senderId)
            .orElseThrow { ServiceException("채팅방에 접근할 권한이 없습니다.") }

        // 채팅방에 메시지가 있는지 확인 (첫 번째 메시지인지 체크)
        val isFirstMessage = chatMessageRepository.countByRoomId(request.roomId) == 0L

        // 첫 번째 메시지인 경우 시스템 메시지 생성
        if (isFirstMessage) {
            try {
                val rent = rentRepository.findById(chatRoom.rentId).orElse(null)
                rent?.let {
                    val systemMessage = "📚 '${it.bookTitle}' 책에 대한 채팅방이 생성되었습니다."
                    createSystemMessage(request.roomId, systemMessage)
                }
            } catch (e: Exception) {
                log.warn("시스템 메시지 생성 실패 - roomId: {}", request.roomId, e)
            }
        }

        val message = createChatMessage(request.roomId, senderId, request.content, request.messageType)

        chatMessageRepository.save(message)

        chatRoom.updateLastMessage(request.content, LocalDateTime.now())
        chatRoomRepository.save(chatRoom)

        val sender = userRepository.findById(senderId).orElse(null)
        val senderNickname = sender?.nickname ?: "알 수 없는 사용자"

        // 메시지를 보낸 사람이므로 항상 isMine = true
        val response = MessageResponse.from(message, senderNickname, null, true)

        log.info("메시지 전송 완료 - messageId: {}, senderId: {}", message.id, senderId)
        return response
    }

    @Transactional
    fun markMessagesAsRead(roomId: String, userId: Long) {
        log.info("메시지 읽음 처리 - roomId: {}, userId: {}", roomId, userId)

        chatRoomRepository.findByRoomIdAndUserId(roomId, userId)
            .orElseThrow { ServiceException("채팅방에 접근할 권한이 없습니다.") }

        val updatedCount = chatMessageRepository.markAllMessagesAsReadInRoom(roomId, userId)
        log.info("읽음 처리 완료 - 업데이트된 메시지 수: {}", updatedCount)
    }

    fun getUnreadMessageCount(userId: Long): Long {
        // 사용자가 참여한 모든 채팅방 중 나가지 않은 채팅방에서만 읽지 않은 메시지 카운트
        val userChatRooms = chatRoomRepository.findByLenderIdOrBorrowerId(userId, userId)

        return userChatRooms
            .filter { !it.hasUserLeft(userId) }  // 사용자가 나간 채팅방은 제외
            .sumOf { chatRoom ->
                // 해당 채팅방의 읽지 않은 메시지 수 조회
                chatMessageRepository.countUnreadMessagesByRoomIdAndUserId(chatRoom.roomId, userId)
            }
    }

    @Transactional
    fun leaveChatRoom(roomId: String, userId: Long) {
        log.info("채팅방 나가기 시작 - roomId: {}, userId: {}", roomId, userId)

        // 채팅방 존재 여부 및 권한 확인
        val chatRoom = chatRoomRepository.findByRoomIdAndUserId(roomId, userId)
            .orElseThrow {
                log.error("채팅방 접근 권한 없음 - roomId: {}, userId: {}", roomId, userId)
                ServiceException("채팅방에 접근할 권한이 없습니다.")
            }

        try {
            // 이미 나간 상태인지 확인
            if (chatRoom.hasUserLeft(userId)) {
                log.warn("이미 나간 채팅방 - roomId: {}, userId: {}", roomId, userId)
                return  // 이미 나간 상태면 그대로 종료
            }

            // 채팅방을 나가기 전에 모든 읽지 않은 메시지를 읽음 처리
            val markedAsReadCount = chatMessageRepository.markAllMessagesAsReadInRoom(roomId, userId)
            log.info("채팅방 나가기 - 읽음 처리된 메시지 수: {}", markedAsReadCount)

            // 상대방에게 나가기 알림 메시지 전송
            val leavingUser = userRepository.findById(userId).orElse(null)
            val leavingUserNickname = leavingUser?.nickname ?: "사용자"
            val leaveMessage = "💔 ${leavingUserNickname}님이 채팅방을 나갔습니다."

            // 시스템 메시지 생성 (실패해도 나가기는 진행)
            try {
                createSystemMessage(roomId, leaveMessage)
            } catch (e: Exception) {
                log.warn("시스템 메시지 생성 실패 - roomId: {}", roomId, e)
            }

            // 해당 사용자만 "나가기" 표시 (채팅방은 유지)
            chatRoom.markUserAsLeft(userId)
            val savedRoom = chatRoomRepository.save(chatRoom)

            log.info("사용자 채팅방 나가기 완료 - roomId: {}, userId: {}, isEmpty: {}", 
                roomId, userId, savedRoom.isEmpty)
        } catch (e: Exception) {
            log.error("채팅방 나가기 중 오류 발생 - roomId: {}, userId: {}", roomId, userId, e)
            throw ServiceException("채팅방 나가기 중 오류가 발생했습니다: ${e.message}")
        }
    }

    @Transactional
    fun createSystemMessage(roomId: String, content: String) {
        saveSystemMessageAndUpdateRoom(roomId, content)
    }

    @Suppress("unused") // 향후 북카드 메시지 기능에서 사용 예정
    @Transactional
    fun createBookCardMessage(
        roomId: String,
        rentId: Long,
        bookTitle: String,
        bookImage: String?,
        message: String
    ) {
        val jsonContent = """
            {"type":"BOOK_CARD","rentId":$rentId,"bookTitle":"$bookTitle","bookImage":"${bookImage ?: ""}","message":"$message"}
        """.trimIndent()

        saveSystemMessageAndUpdateRoom(roomId, jsonContent)
    }

    private fun buildChatRoomResponse(
        room: ChatRoom,
        rent: Rent,
        lender: User,
        borrower: User,
        currentUserId: Long
    ): ChatRoomResponse {
        val isCurrentUserLender = room.lenderId == currentUserId
        val otherUser = if (isCurrentUserLender) borrower else lender
        val otherUserId = otherUser.id

        val unreadCount = chatMessageRepository.countUnreadMessagesByRoomIdAndUserId(room.roomId, currentUserId)

        return ChatRoomResponse.from(
            room,
            rent.bookTitle,
            rent.bookImage,
            otherUser.nickname,
            null,
            unreadCount
        ).copy(otherUserId = otherUserId)
    }

    private fun saveSystemMessageAndUpdateRoom(roomId: String, content: String, messageType: MessageType = MessageType.SYSTEM) {
        val systemMessage = createChatMessage(roomId, 0L, content, messageType)
        chatMessageRepository.save(systemMessage)

        val chatRoom = chatRoomRepository.findByRoomId(roomId).orElse(null)
        chatRoom?.let {
            val displayMessage = if (messageType == MessageType.SYSTEM && content.startsWith("{")) {
                // JSON 형태의 메시지인 경우 message 부분만 추출하여 표시
                content.substringAfter("\"message\":\"").substringBefore("\"}")
            } else {
                content
            }
            it.updateLastMessage(displayMessage, LocalDateTime.now())
            chatRoomRepository.save(it)
        }
    }

    private fun createChatMessage(roomId: String, senderId: Long, content: String, messageType: MessageType): ChatMessage {
        return ChatMessage().apply {
            this.roomId = roomId
            this.senderId = senderId
            this.content = content
            this.messageType = messageType
            this.isRead = false
            this.createdDate = LocalDateTime.now()
        }
    }

    private fun getRentInfo(rentId: Long): Pair<String, String?> {
        val rent = rentRepository.findById(rentId).orElse(null)
        val bookTitle = rent?.bookTitle ?: "알 수 없는 책"
        val bookImage = rent?.bookImage
        return Pair(bookTitle, bookImage)
    }
    @Transactional
    fun cleanupDuplicateChatRooms(duplicateRooms: MutableList<ChatRoom>, protectedRoom: ChatRoom?) {
        if (duplicateRooms.size <= 1) {
            return  // 중복이 없으면 처리할 필요 없음
        }

        try {
            for (duplicateRoom in duplicateRooms) {
                // 보호할 채팅방은 건드리지 않음
                if (protectedRoom != null && duplicateRoom.id == protectedRoom.id) {
                    continue
                }

                // 활성이고 사용자가 모두 있는 방도 보호
                if (duplicateRoom.isActive && 
                    !duplicateRoom.hasUserLeft(duplicateRoom.lenderId) && 
                    !duplicateRoom.hasUserLeft(duplicateRoom.borrowerId)) {
                    continue
                }

                log.info("중복 채팅방 정리 중 - roomId: {}, createdDate: {}, active: {}", 
                    duplicateRoom.roomId, duplicateRoom.createdDate, duplicateRoom.isActive)

                try {
                    // 1. 관련 메시지들 먼저 삭제
                    val deletedMessages = chatMessageRepository.deleteByRoomId(duplicateRoom.roomId)
                    log.info("채팅방 메시지 삭제 완료 - roomId: {}, 삭제된 메시지 수: {}", 
                        duplicateRoom.roomId, deletedMessages)

                    // 2. 채팅방 삭제
                    chatRoomRepository.delete(duplicateRoom)
                    log.info("중복 채팅방 삭제 완료 - roomId: {}", duplicateRoom.roomId)
                } catch (e: Exception) {
                    log.error("개별 채팅방 삭제 실패 - roomId: {}", duplicateRoom.roomId, e)
                    // 개별 실패는 전체 프로세스를 중단시키지 않음
                }
            }

            log.info("중복 채팅방 정리 완료")
        } catch (e: Exception) {
            log.error("중복 채팅방 정리 중 전체 오류 발생", e)
            // 정리 실패해도 채팅방 생성은 진행
        }
    }
}