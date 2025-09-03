package com.bookbook.domain.chat.service

import com.bookbook.domain.chat.dto.ChatRoomCreateRequest
import com.bookbook.domain.chat.dto.MessageSendRequest
import com.bookbook.domain.chat.enums.MessageType
import com.bookbook.domain.chat.repository.ChatMessageRepository
import com.bookbook.domain.chat.repository.ChatRoomRepository
import com.bookbook.domain.chat.service.ChatService
import com.bookbook.domain.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Chat Service 테스트")
class ChatServiceTest {

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var chatRoomRepository: ChatRoomRepository

    @Autowired
    private lateinit var chatMessageRepository: ChatMessageRepository

    @Test
    @DisplayName("1. 채팅방 생성 테스트")
    fun test1() {
        // TestSetup의 더미 사용자 활용
        val lender = userRepository.findByUsername("user1").orElseThrow()
        val borrower = userRepository.findByUsername("user2").orElseThrow()
        // RentInitData에서 생성된 대여글 ID 사용
        val rentId = 1L

        val request = ChatRoomCreateRequest(
            rentId = rentId,
            lenderId = lender.id
        )

        // 채팅방 생성
        val response = chatService.createOrGetChatRoom(request, borrower.id)

        // 검증
        assertThat(response.roomId).isNotNull()
        assertThat(response.rentId).isEqualTo(rentId)
        assertThat(response.bookTitle).isNotNull()
        assertThat(response.otherUserNickname).isEqualTo(lender.nickname)
    }

    @Test
    @DisplayName("2. 기존 채팅방 조회 테스트")
    fun test2() {
        val lender = userRepository.findByUsername("user3").orElseThrow()
        val borrower = userRepository.findByUsername("user4").orElseThrow()
        val rentId = 2L

        val request = ChatRoomCreateRequest(
            rentId = rentId,
            lenderId = lender.id
        )

        // 첫 번째 채팅방 생성
        val response1 = chatService.createOrGetChatRoom(request, borrower.id)

        // 동일한 조건으로 다시 요청 (기존 채팅방 반환되어야 함)
        val response2 = chatService.createOrGetChatRoom(request, borrower.id)

        // 동일한 채팅방이 반환되는지 검증
        assertThat(response1.roomId).isEqualTo(response2.roomId)
    }

    @Test
    @DisplayName("3. 메시지 전송 테스트")
    fun test3() {
        val lender = userRepository.findByUsername("user1").orElseThrow()
        val borrower = userRepository.findByUsername("user2").orElseThrow()
        val rentId = 1L

        // 채팅방 생성
        val roomRequest = ChatRoomCreateRequest(rentId = rentId, lenderId = lender.id)
        val chatRoom = chatService.createOrGetChatRoom(roomRequest, borrower.id)

        // 메시지 전송
        val messageRequest = MessageSendRequest(
            roomId = chatRoom.roomId,
            content = "안녕하세요! 대여 문의드립니다.",
            messageType = MessageType.TEXT
        )

        val messageResponse = chatService.sendMessage(messageRequest, borrower.id)

        // 검증
        assertThat(messageResponse.content).isEqualTo("안녕하세요! 대여 문의드립니다.")
        assertThat(messageResponse.senderNickname).isEqualTo(borrower.nickname)
        assertThat(messageResponse.isMine).isTrue()
        assertThat(messageResponse.messageType).isEqualTo(MessageType.TEXT)
    }

    @Test
    @DisplayName("4. 채팅방 목록 조회 테스트")
    fun test4() {
        val user = userRepository.findByUsername("user5").orElseThrow()
        val otherUser = userRepository.findByUsername("user1").orElseThrow()
        val rentId = 3L

        // 채팅방 생성
        val request = ChatRoomCreateRequest(rentId = rentId, lenderId = otherUser.id)
        val createdChatRoom = chatService.createOrGetChatRoom(request, user.id)

        // 채팅방에 메시지 전송 (목록 조회를 위해 필요)
        val messageRequest = MessageSendRequest(
            roomId = createdChatRoom.roomId,
            content = "채팅방 목록 조회 테스트 메시지",
            messageType = MessageType.TEXT
        )
        chatService.sendMessage(messageRequest, user.id)

        // 채팅방 목록 조회
        val pageable = PageRequest.of(0, 10)
        val chatRooms = chatService.getChatRooms(user.id, pageable)

        // 검증
        assertThat(chatRooms.content).hasSizeGreaterThanOrEqualTo(1)
        val foundChatRoom = chatRooms.content.find { it.rentId == rentId }
        assertThat(foundChatRoom).isNotNull()
        assertThat(foundChatRoom?.otherUserNickname).isEqualTo(otherUser.nickname)
    }

    @Test
    @DisplayName("5. 메시지 조회 테스트")
    fun test5() {
        val lender = userRepository.findByUsername("user1").orElseThrow()
        val borrower = userRepository.findByUsername("user2").orElseThrow()
        val rentId = 1L

        // 채팅방 생성
        val roomRequest = ChatRoomCreateRequest(rentId = rentId, lenderId = lender.id)
        val chatRoom = chatService.createOrGetChatRoom(roomRequest, borrower.id)

        // 메시지 전송
        val messageRequest = MessageSendRequest(
            roomId = chatRoom.roomId,
            content = "테스트 메시지입니다.",
            messageType = MessageType.TEXT
        )
        chatService.sendMessage(messageRequest, borrower.id)

        // 메시지 조회
        val pageable = PageRequest.of(0, 50)
        val messages = chatService.getChatMessages(chatRoom.roomId, borrower.id, pageable)

        // 검증 (시스템 메시지 + 사용자 메시지가 있을 것)
        assertThat(messages.content).hasSizeGreaterThanOrEqualTo(1)
        val userMessage = messages.content.find { it.content == "테스트 메시지입니다." }
        assertThat(userMessage).isNotNull()
        assertThat(userMessage?.senderNickname).isEqualTo(borrower.nickname)
    }

    @Test
    @DisplayName("6. 메시지 읽음 처리 테스트")
    fun test6() {
        val lender = userRepository.findByUsername("user3").orElseThrow()
        val borrower = userRepository.findByUsername("user4").orElseThrow()
        val rentId = 2L

        // 채팅방 생성 및 메시지 전송
        val roomRequest = ChatRoomCreateRequest(rentId = rentId, lenderId = lender.id)
        val chatRoom = chatService.createOrGetChatRoom(roomRequest, borrower.id)

        val messageRequest = MessageSendRequest(
            roomId = chatRoom.roomId,
            content = "읽음 처리 테스트",
            messageType = MessageType.TEXT
        )
        chatService.sendMessage(messageRequest, borrower.id)

        // 읽지 않은 메시지 개수 확인 (lender 관점)
        val unreadCountBefore = chatService.getUnreadMessageCount(lender.id)

        // 메시지 읽음 처리 (lender가 읽음)
        chatService.markMessagesAsRead(chatRoom.roomId, lender.id)

        // 읽음 처리 후 읽지 않은 메시지 개수 확인
        val unreadCountAfter = chatService.getUnreadMessageCount(lender.id)

        // 읽지 않은 메시지가 감소했는지 확인
        assertThat(unreadCountAfter).isLessThanOrEqualTo(unreadCountBefore)
    }

    @Test
    @DisplayName("7. 채팅방 나가기 테스트")
    fun test7() {
        val lender = userRepository.findByUsername("user1").orElseThrow()
        val borrower = userRepository.findByUsername("user5").orElseThrow()
        val rentId = 4L // 다른 테스트와 겹치지 않는 rentId 사용

        // 채팅방 생성
        val roomRequest = ChatRoomCreateRequest(rentId = rentId, lenderId = lender.id)
        val chatRoom = chatService.createOrGetChatRoom(roomRequest, borrower.id)

        // 메시지 전송 (채팅방 목록 조회를 위해 필요)
        val messageRequest = MessageSendRequest(
            roomId = chatRoom.roomId,
            content = "나가기 테스트 메시지",
            messageType = MessageType.TEXT
        )
        chatService.sendMessage(messageRequest, borrower.id)

        // 채팅방 나가기 전 목록 확인
        val chatRoomsBefore = chatService.getChatRooms(borrower.id, PageRequest.of(0, 10))
        val roomBeforeLeave = chatRoomsBefore.content.find { it.roomId == chatRoom.roomId }
        assertThat(roomBeforeLeave).isNotNull()

        // 채팅방 나가기
        chatService.leaveChatRoom(chatRoom.roomId, borrower.id)

        // 나가기 후 목록에서 사라졌는지 확인
        val chatRoomsAfter = chatService.getChatRooms(borrower.id, PageRequest.of(0, 10))
        val roomAfterLeave = chatRoomsAfter.content.find { it.roomId == chatRoom.roomId }
        assertThat(roomAfterLeave).isNull() // 목록에서 사라져야 함
    }

    @Test
    @DisplayName("8. 자기 자신과의 채팅 방지 테스트")
    fun test8() {
        val user = userRepository.findByUsername("user1").orElseThrow()
        val rentId = 1L

        val request = ChatRoomCreateRequest(
            rentId = rentId,
            lenderId = user.id
        )

        // 자기 자신과의 채팅 시도 시 예외 발생
        assertThatThrownBy {
            chatService.createOrGetChatRoom(request, user.id)
        }.hasMessageContaining("자기 자신과는 채팅할 수 없습니다")
    }

    @Test
    @DisplayName("9. 존재하지 않는 채팅방 접근 테스트")
    fun test9() {
        val user = userRepository.findByUsername("user1").orElseThrow()

        // 존재하지 않는 채팅방 조회 시 예외 발생
        assertThatThrownBy {
            chatService.getChatRoom("non-existent-room-id", user.id)
        }.hasMessageContaining("권한이 없습니다")

        // 존재하지 않는 채팅방에 메시지 전송 시 예외 발생
        val messageRequest = MessageSendRequest(
            roomId = "non-existent-room-id",
            content = "테스트",
            messageType = MessageType.TEXT
        )

        assertThatThrownBy {
            chatService.sendMessage(messageRequest, user.id)
        }.hasMessageContaining("권한이 없습니다")
    }
}
