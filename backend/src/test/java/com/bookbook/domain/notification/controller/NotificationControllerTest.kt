package com.bookbook.domain.notification.controller

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Notification Controller 테스트")
class NotificationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @DisplayName("1. 알림 목록 조회 - Spring Security 인증 차단")
    fun test1() {
        mockMvc.perform(get("/api/v1/bookbook/user/notifications"))
            .andDo(print())
            .andExpect(status().isUnauthorized) // 401 (Spring Security authenticationEntryPoint)
    }

    @Test
    @DisplayName("2. 읽지 않은 알림 개수 조회 - Spring Security 인증 차단")
    fun test2() {
        mockMvc.perform(get("/api/v1/bookbook/user/notifications/unread-count"))
            .andDo(print())
            .andExpect(status().isUnauthorized) // 401
    }

    @Test
    @DisplayName("3. 알림 읽음 처리 - Spring Security 인증 차단")
    fun test3() {
        mockMvc.perform(patch("/api/v1/bookbook/user/notifications/1/read"))
            .andDo(print())
            .andExpect(status().isUnauthorized) // 401
    }

    @Test
    @DisplayName("4. 모든 알림 읽음 처리 - Spring Security 인증 차단")
    fun test4() {
        mockMvc.perform(patch("/api/v1/bookbook/user/notifications/read-all"))
            .andDo(print())
            .andExpect(status().isUnauthorized) // 401
    }

    @Test
    @DisplayName("5. 알림 삭제 - Spring Security 인증 차단")
    fun test5() {
        mockMvc.perform(delete("/api/v1/bookbook/user/notifications/1"))
            .andDo(print())
            .andExpect(status().isUnauthorized) // 401
    }

    @Test
    @DisplayName("6. 대여 신청 상세 정보 조회 - Spring Security 인증 차단")
    fun test6() {
        mockMvc.perform(get("/api/v1/bookbook/user/notifications/1/rent-request-detail"))
            .andDo(print())
            .andExpect(status().isUnauthorized) // 401
    }
}
