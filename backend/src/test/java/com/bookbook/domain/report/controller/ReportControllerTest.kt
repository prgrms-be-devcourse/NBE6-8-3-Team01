package com.bookbook.domain.report.controller

import com.bookbook.TestSetup
import com.bookbook.domain.report.dto.request.ReportRequestDto
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ReportController 통합 테스트")
@Transactional
class ReportControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var testSetup: TestSetup

    private val objectMapper = ObjectMapper()

    private lateinit var reporterUser: User
    private lateinit var targetUser: User

    @BeforeEach
    fun setUp() {
        reporterUser = userRepository.save(User(
            username = "reporterUser",
            password = "password",
            nickname = "reporter",
            email = "reporter@test.com",
            address = "서울시",
            registrationCompleted = true
        ))

        targetUser = userRepository.save(User(
            username = "targetUser",
            password = "password",
            nickname = "target",
            email = "target@test.com",
            address = "서울시",
            registrationCompleted = true
        ))

        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("신고 성공 테스트")
    fun report_success_test() {
        val requestDto = ReportRequestDto(targetUserId = targetUser.id, reason = "불쾌한 콘텐츠 게시")
        val customOAuth2User = testSetup.createCustomOAuth2User(reporterUser)
        val authentication = UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.authorities)
        SecurityContextHolder.getContext().authentication = authentication

        mockMvc.perform(
            post("/api/v1/bookbook/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-OK"))
            .andExpect(jsonPath("$.msg").value("신고가 성공적으로 접수되었습니다."))
    }

    @Test
    @DisplayName("유효성 검사 실패 테스트")
    fun report_validation_fail_test() {
        val requestDto = ReportRequestDto(targetUserId = -1L, reason = "")
        val customOAuth2User = testSetup.createCustomOAuth2User(reporterUser)
        val authentication = UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.authorities)
        SecurityContextHolder.getContext().authentication = authentication

        mockMvc.perform(
            post("/api/v1/bookbook/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-1"))
            .andExpect(jsonPath("$.msg").isString)
    }

    @Test
    @DisplayName("인증 실패 테스트")
    fun report_unauthorized_test() {
        val targetUserId = targetUser.id
        val reason = "불쾌한 콘텐츠 게시"
        val requestDto = ReportRequestDto(targetUserId = targetUserId, reason = reason)

        SecurityContextHolder.getContext().authentication = null

        mockMvc.perform(
            post("/api/v1/bookbook/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        )
            .andExpect(status().isUnauthorized)
    }
}