package com.bookbook.domain.notification.service

import com.bookbook.domain.notification.enums.NotificationType
import com.bookbook.domain.notification.repository.NotificationRepository
import com.bookbook.domain.notification.service.NotificationService
import com.bookbook.domain.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Notification Service 테스트")
class NotificationServiceTest {

    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    @DisplayName("1. 알림 생성 및 조회 테스트")
    fun test1() {
        // TestSetup의 더미 사용자 활용
        val receiver = userRepository.findByUsername("user1").orElseThrow()
        val sender = userRepository.findByUsername("user2").orElseThrow()

        // 알림 생성
        notificationService.createNotification(
            receiver = receiver,
            sender = sender,
            type = NotificationType.RENT_REQUEST,
            message = "테스트 알림 메시지",
            bookTitle = "테스트 도서",
            bookImageUrl = "/test-image.jpg",
            relatedId = null
        )

        // 알림 조회
        val notifications = notificationService.getNotificationsByUser(receiver)

        // 검증
        assertThat(notifications).hasSizeGreaterThanOrEqualTo(1)
        assertThat(notifications[0].detailMessage).isEqualTo("테스트 알림 메시지")
        assertThat(notifications[0].requester).isEqualTo(sender.nickname)
        assertThat(notifications[0].read).isFalse()
    }

    @Test
    @DisplayName("2. 읽지 않은 알림 개수 테스트")
    fun test2() {
        val receiver = userRepository.findByUsername("user3").orElseThrow()
        val sender = userRepository.findByUsername("user1").orElseThrow()

        // 읽지 않은 알림 여러 개 생성
        repeat(3) {
            notificationService.createNotification(
                receiver = receiver,
                sender = sender,
                type = NotificationType.RENT_REQUEST,
                message = "읽지 않은 알림 $it",
                bookTitle = null,
                bookImageUrl = null,
                relatedId = null
            )
        }

        // 읽지 않은 알림 개수 확인
        val unreadCount = notificationService.getUnreadCount(receiver)
        assertThat(unreadCount).isGreaterThanOrEqualTo(3)
    }

    @Test
    @DisplayName("3. 알림 읽음 처리 테스트")
    fun test3() {
        val receiver = userRepository.findByUsername("user4").orElseThrow()
        val sender = userRepository.findByUsername("user1").orElseThrow()

        // 알림 생성
        val notification = notificationService.createNotification(
            receiver = receiver,
            sender = sender,
            type = NotificationType.RENT_REQUEST,
            message = "읽음 처리 테스트",
            bookTitle = null,
            bookImageUrl = null,
            relatedId = null
        )

        // 읽음 처리 전 확인
        val unreadCountBefore = notificationService.getUnreadCount(receiver)
        assertThat(unreadCountBefore).isGreaterThanOrEqualTo(1)

        // 읽음 처리
        notificationService.markAsRead(notification.id, receiver)

        // 읽음 처리 후 확인 (감소했는지 확인)
        val unreadCountAfter = notificationService.getUnreadCount(receiver)
        assertThat(unreadCountAfter).isLessThan(unreadCountBefore)

        // 알림 상태 확인
        val notifications = notificationService.getNotificationsByUser(receiver)
        val readNotification = notifications.find { it.id == notification.id }
        assertThat(readNotification?.read).isTrue()
    }

    @Test
    @DisplayName("4. 모든 알림 읽음 처리 테스트")
    fun test4() {
        val receiver = userRepository.findByUsername("user5").orElseThrow()
        val sender = userRepository.findByUsername("user1").orElseThrow()

        // 여러 알림 생성
        repeat(3) {
            notificationService.createNotification(
                receiver = receiver,
                sender = sender,
                type = NotificationType.RENT_REQUEST,
                message = "일괄 읽음 테스트 $it",
                bookTitle = null,
                bookImageUrl = null,
                relatedId = null
            )
        }

        // 읽음 처리 전 확인
        val unreadCountBefore = notificationService.getUnreadCount(receiver)
        assertThat(unreadCountBefore).isGreaterThanOrEqualTo(3)

        // 모든 알림 읽음 처리
        notificationService.markAllAsRead(receiver)

        // 읽음 처리 후 확인
        val unreadCountAfter = notificationService.getUnreadCount(receiver)
        assertThat(unreadCountAfter).isEqualTo(0)
    }

    @Test
    @DisplayName("5. 알림 삭제 테스트")
    fun test5() {
        val receiver = userRepository.findByUsername("user2").orElseThrow()
        val sender = userRepository.findByUsername("user1").orElseThrow()

        // 삭제 전 알림 개수 확인
        val notificationsBefore = notificationService.getNotificationsByUser(receiver)
        val countBefore = notificationsBefore.size

        // 알림 생성
        val notification = notificationService.createNotification(
            receiver = receiver,
            sender = sender,
            type = NotificationType.RENT_REQUEST,
            message = "삭제 테스트",
            bookTitle = null,
            bookImageUrl = null,
            relatedId = null
        )

        // 생성 후 확인
        val notificationsAfterCreate = notificationService.getNotificationsByUser(receiver)
        assertThat(notificationsAfterCreate).hasSizeGreaterThan(countBefore)

        // 알림 삭제
        notificationService.deleteNotification(notification.id, receiver)

        // 삭제 후 확인
        val notificationsAfterDelete = notificationService.getNotificationsByUser(receiver)
        assertThat(notificationsAfterDelete).hasSize(countBefore)
    }

    @Test
    @DisplayName("6. 대여 신청 알림 생성 및 상세 정보 조회 테스트")
    fun test6() {
        val bookOwner = userRepository.findByUsername("user1").orElseThrow()
        val requester = userRepository.findByUsername("user2").orElseThrow()
        // RentInitData에서 생성된 대여글 ID 사용
        val rentId = 1L

        // 대여 신청 알림 생성
        val notification = notificationService.createNotification(
            receiver = bookOwner,
            sender = requester,
            type = NotificationType.RENT_REQUEST,
            message = "대여 신청 드립니다",
            bookTitle = "테스트 도서",
            bookImageUrl = "/test-book.jpg",
            relatedId = rentId
        )

        // 대여 신청 상세 정보 조회
        val detail = notificationService.getRentRequestDetail(notification.id, bookOwner)

        // 검증
        assertThat(detail["rentId"]).isEqualTo(rentId)
        assertThat(detail["bookTitle"]).isNotNull()
        assertThat(detail).containsKey("isProcessable")
        assertThat(detail).containsKey("processStatus")
    }

    @Test
    @DisplayName("7. 다른 사용자 알림 접근 시 예외 테스트")
    fun test7() {
        val receiver = userRepository.findByUsername("user1").orElseThrow()
        val otherUser = userRepository.findByUsername("user2").orElseThrow()
        val sender = userRepository.findByUsername("user3").orElseThrow()

        // receiver에게 알림 생성
        val notification = notificationService.createNotification(
            receiver = receiver,
            sender = sender,
            type = NotificationType.RENT_REQUEST,
            message = "권한 테스트",
            bookTitle = null,
            bookImageUrl = null,
            relatedId = null
        )

        // 다른 사용자가 읽음 처리 시도 시 예외 발생
        assertThatThrownBy {
            notificationService.markAsRead(notification.id, otherUser)
        }.hasMessageContaining("다른 사용자의 알림에 접근할 수 없습니다")

        // 다른 사용자가 삭제 시도 시 예외 발생
        assertThatThrownBy {
            notificationService.deleteNotification(notification.id, otherUser)
        }.hasMessageContaining("다른 사용자의 알림을 삭제할 수 없습니다")
    }

    @Test
    @DisplayName("8. 존재하지 않는 알림 접근 시 예외 테스트")
    fun test8() {
        val user = userRepository.findByUsername("user1").orElseThrow()

        // 존재하지 않는 알림 ID로 읽음 처리 시도
        assertThatThrownBy {
            notificationService.markAsRead(99999L, user)
        }.hasMessageContaining("존재하지 않는 알림입니다")

        // 존재하지 않는 알림 ID로 삭제 시도
        assertThatThrownBy {
            notificationService.deleteNotification(99999L, user)
        }.hasMessageContaining("존재하지 않는 알림입니다")
    }
}
