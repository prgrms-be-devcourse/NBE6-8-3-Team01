package com.bookbook.domain.notification

import com.bookbook.TestSetup
import com.bookbook.domain.notification.entity.Notification
import com.bookbook.domain.notification.enums.NotificationType
import com.bookbook.domain.notification.repository.NotificationRepository
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Notification API 통합 테스트")
class NotificationApiTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var testSetup: TestSetup

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Test
    @DisplayName("1-1. 사용자 알림 조회 (로그인)")
    fun test1_1() {
        // TestSetup에서 생성된 사용자 사용
        val user = userRepository.findByUsername("user1").orElseThrow()
        val customOAuth2User = testSetup.createCustomOAuth2User(user)

        // 테스트용 알림 생성
        createTestNotification(user)

        mockMvc.perform(get("/api/v1/bookbook/user/notifications")
                .with(authentication(customOAuth2User)))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("알림 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    @DisplayName("1-2. 사용자 알림 조회 (비로그인)")
    fun test1_2() {
        mockMvc.perform(get("/api/v1/bookbook/user/notifications"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("401-1"))
            .andExpect(jsonPath("$.msg").value("로그인 후 사용해주세요."))
    }

    @Test
    @DisplayName("2-1. 읽지 않은 알림 개수 조회 (로그인)")
    fun test2_1() {
        val user = userRepository.findByUsername("user2").orElseThrow()
        val customOAuth2User = testSetup.createCustomOAuth2User(user)

        // 읽지 않은 알림 생성
        createTestNotification(user)

        mockMvc.perform(get("/api/v1/bookbook/user/notifications/unread-count")
                .with(authentication(customOAuth2User)))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("읽지 않은 알림 개수를 조회했습니다."))
            .andExpect(jsonPath("$.data").isNumber)
    }

    @Test
    @DisplayName("2-2. 읽지 않은 알림 개수 조회 (비로그인)")
    fun test2_2() {
        mockMvc.perform(get("/api/v1/bookbook/user/notifications/unread-count"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("401-1"))
            .andExpect(jsonPath("$.msg").value("로그인 후 사용해주세요."))
    }

    @Test
    @DisplayName("3. 알림 읽음 처리 테스트")
    fun test3() {
        val user = userRepository.findByUsername("user3").orElseThrow()
        val customOAuth2User = testSetup.createCustomOAuth2User(user)

        // 테스트용 알림 생성
        val notification = createTestNotification(user)

        mockMvc.perform(patch("/api/v1/bookbook/user/notifications/${notification.id}/read")
                .with(authentication(customOAuth2User)))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("알림을 읽음 처리했습니다."))
    }

    @Test
    @DisplayName("4. 모든 알림 읽음 처리 테스트")
    fun test4() {
        val user = userRepository.findByUsername("user4").orElseThrow()
        val customOAuth2User = testSetup.createCustomOAuth2User(user)

        // 여러 테스트용 알림 생성
        createTestNotification(user)
        createTestNotification(user)

        mockMvc.perform(patch("/api/v1/bookbook/user/notifications/read-all")
                .with(authentication(customOAuth2User)))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("모든 알림을 읽음 처리했습니다."))
    }

    @Test
    @DisplayName("5. 알림 삭제 테스트")
    fun test5() {
        val user = userRepository.findByUsername("user5").orElseThrow()
        val customOAuth2User = testSetup.createCustomOAuth2User(user)

        // 테스트용 알림 생성
        val notification = createTestNotification(user)

        mockMvc.perform(delete("/api/v1/bookbook/user/notifications/${notification.id}")
                .with(authentication(customOAuth2User)))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("알림을 삭제했습니다."))
    }

    @Test
    @DisplayName("6. 대여 신청 상세 정보 조회 테스트")
    fun test6() {
        val user = userRepository.findByUsername("user1").orElseThrow()
        val customOAuth2User = testSetup.createCustomOAuth2User(user)

        // RentInitData에서 생성된 대여글 ID 사용 (1번 대여글)
        val rentId = 1L
        val notification = createRentRequestNotification(user, rentId)

        mockMvc.perform(get("/api/v1/bookbook/user/notifications/${notification.id}/rent-request-detail")
                .with(authentication(customOAuth2User)))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("대여 신청 상세 정보를 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
    }

    @Test
    @DisplayName("7. API 응답 구조 검증 테스트")
    fun test7() {
        val user = userRepository.findByUsername("user1").orElseThrow()
        val customOAuth2User = testSetup.createCustomOAuth2User(user)

        // 7-1. 알림 목록 응답 구조 검증
        mockMvc.perform(get("/api/v1/bookbook/user/notifications")
                .with(authentication(customOAuth2User)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").exists())
            .andExpect(jsonPath("$.msg").exists())
            .andExpect(jsonPath("$.data").exists())

        // 7-2. 읽지 않은 알림 개수 응답 구조 검증
        mockMvc.perform(get("/api/v1/bookbook/user/notifications/unread-count")
                .with(authentication(customOAuth2User)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").exists())
            .andExpect(jsonPath("$.data").exists())
    }

    // 테스트용 일반 알림 생성 헬퍼 메서드
    private fun createTestNotification(user: User): Notification {
        val sender = userRepository.findByUsername("admin-test").orElseThrow()
        
        val notification = Notification(
            recipient = user,
            sender = sender,
            type = NotificationType.GENERAL,
            message = "테스트 알림입니다.",
            relatedTitle = "테스트 제목",
            relatedImage = null,
            relatedId = null
        )
        
        return notificationRepository.save(notification)
    }

    // 테스트용 대여 신청 알림 생성 헬퍼 메서드
    private fun createRentRequestNotification(user: User, rentId: Long): Notification {
        val sender = userRepository.findByUsername("user2").orElseThrow()
        
        val notification = Notification(
            recipient = user,
            sender = sender,
            type = NotificationType.RENT_REQUEST,
            message = "대여 신청 테스트 메시지",
            relatedTitle = "테스트 도서 제목",
            relatedImage = "/test-image.jpg",
            relatedId = rentId
        )
        
        return notificationRepository.save(notification)
    }
}
