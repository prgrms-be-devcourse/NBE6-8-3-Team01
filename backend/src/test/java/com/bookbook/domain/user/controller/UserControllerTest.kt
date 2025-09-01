package com.bookbook.domain.user.controller

import com.bookbook.TestSetup
import com.bookbook.domain.user.dto.request.UserCreateRequestDto
import com.bookbook.domain.user.dto.request.UserUpdateRequestDto
import com.bookbook.domain.user.enums.Role
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.security.CustomOAuth2User
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("UserController 통합 테스트")
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var setup: TestSetup

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var mockCustomUser: CustomOAuth2User

    @BeforeAll
    fun setUp() {
        setup.createDummyUser()
        val user = userRepository.findById(1L).get()

        mockCustomUser = CustomOAuth2User(
            authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
            attributes = mapOf("id" to user.id, "name" to user.username),
            nameAttributeKey = "name",
            username = user.username,
            nickname = user.nickname,
            userId = user.id,
            isRegistrationCompleted = user.registrationCompleted,
            role = Role.USER
        )
    }

    @Test
    @DisplayName("인증된 유저일 때 인증 상태가 true인지 확인")
    @WithMockUser
    fun testIsAuthenticatedTrue() {
        val auth = UsernamePasswordAuthenticationToken(mockCustomUser, null, mockCustomUser.authorities)
        mockMvc.perform(get("/api/v1/bookbook/users/isAuthenticated").with(authentication(auth)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(true))
    }

    @Test
    @DisplayName("비인증 유저일 때 인증 상태가 false인지 확인")
    @WithAnonymousUser
    fun testIsAuthenticatedFalse() {
        mockMvc.perform(get("/api/v1/bookbook/users/isAuthenticated"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(false))
    }

    @Test
    @DisplayName("사용 가능한 닉네임일 때 true를 반환하는지 확인")
    @WithAnonymousUser
    fun testCheckNicknameAvailable() {
        mockMvc.perform(get("/api/v1/bookbook/users/check-nickname")
            .param("nickname", "available"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isAvailable").value(true))
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임일 때 false를 반환하는지 확인")
    @WithAnonymousUser
    fun testCheckNicknameUnavailable() {
        mockMvc.perform(get("/api/v1/bookbook/users/check-nickname")
            .param("nickname", "nickname-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isAvailable").value(false))
    }

    @Test
    @DisplayName("빈 닉네임으로 요청할 경우 예외 발생 확인")
    @WithAnonymousUser
    fun testCheckNicknameEmpty() {
        mockMvc.perform(get("/api/v1/bookbook/users/check-nickname")
            .param("nickname", " "))
            .andExpect(status().is4xxClientError)
            .andExpect(jsonPath("$.resultCode").value("400-NICKNAME-EMPTY"))
    }

    @Test
    @DisplayName("회원가입이 성공적으로 완료되는지 확인")
    @WithMockUser
    fun testCompleteSignupSuccess() {
        val requestDto = UserCreateRequestDto("newnickname", "newaddress")
        val auth = UsernamePasswordAuthenticationToken(mockCustomUser, null, mockCustomUser.authorities)
        mockMvc.perform(post("/api/v1/bookbook/users/signup")
            .with(authentication(auth))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.msg").value("회원가입이 완료되었습니다."))
    }

    @Test
    @DisplayName("닉네임이 공백일 때 유효성 검사 실패 확인")
    @WithMockUser
    fun testCompleteSignupBlankNickname() {
        val requestDto = UserCreateRequestDto(" ", "newaddress")
        val auth = UsernamePasswordAuthenticationToken(mockCustomUser, null, mockCustomUser.authorities)
        mockMvc.perform(post("/api/v1/bookbook/users/signup")
            .with(authentication(auth))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("비인증 상태에서 회원가입 시 예외 발생 확인")
    @WithAnonymousUser
    fun testCompleteSignupNotAuthenticated() {
        val requestDto = UserCreateRequestDto("newnickname", "newaddress")
        mockMvc.perform(post("/api/v1/bookbook/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("인증된 유저일 때 자신의 정보 조회 확인")
    @WithMockUser
    fun testGetCurrentUserAuthenticated() {
        val auth = UsernamePasswordAuthenticationToken(mockCustomUser, null, mockCustomUser.authorities)
        mockMvc.perform(get("/api/v1/bookbook/users/me")
            .with(authentication(auth)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.username").value("user1"))
    }

    @Test
    @DisplayName("비인증 상태에서 자신의 정보 조회 시 예외 발생 확인")
    @WithAnonymousUser
    fun testGetCurrentUserNotAuthenticated() {
        mockMvc.perform(get("/api/v1/bookbook/users/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("인증된 유저일 때 자신의 상태 조회 확인")
    @WithMockUser
    fun testGetCurrentUserStatusAuthenticated() {
        val auth = UsernamePasswordAuthenticationToken(mockCustomUser, null, mockCustomUser.authorities)
        mockMvc.perform(get("/api/v1/bookbook/users/status")
            .with(authentication(auth)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userStatus").value("ACTIVE"))
    }

    @Test
    @DisplayName("비인증 상태에서 자신의 상태 조회 시 예외 발생 확인")
    @WithAnonymousUser
    fun testGetCurrentUserStatusNotAuthenticated() {
        mockMvc.perform(get("/api/v1/bookbook/users/status"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("회원 탈퇴가 성공적으로 처리되는지 확인")
    @WithMockUser
    fun testDeactivateUserSuccess() {
        val auth = UsernamePasswordAuthenticationToken(mockCustomUser, null, mockCustomUser.authorities)
        mockMvc.perform(delete("/api/v1/bookbook/users/me")
            .with(authentication(auth)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.msg").value("회원 탈퇴가 성공적으로 처리되었습니다."))
    }

    @Test
    @DisplayName("비인증 상태에서 회원 탈퇴 시도 시 예외 발생 확인")
    @WithAnonymousUser
    fun testDeactivateUserNotAuthenticated() {
        mockMvc.perform(delete("/api/v1/bookbook/users/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("회원 정보(닉네임, 주소)가 성공적으로 수정되는지 확인")
    @WithMockUser
    fun testUpdateMyInfoSuccess() {
        val requestDto = UserUpdateRequestDto("newnickname", "newaddress")
        val auth = UsernamePasswordAuthenticationToken(mockCustomUser, null, mockCustomUser.authorities)
        mockMvc.perform(patch("/api/v1/bookbook/users/me")
            .with(authentication(auth))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.msg").value("회원 정보가 성공적으로 수정되었습니다."))
    }

    @Test
    @DisplayName("수정할 정보가 없을 때 예외 발생 확인")
    @WithMockUser
    fun testUpdateMyInfoNoChanges() {
        val requestDto = UserUpdateRequestDto(null, null)
        val auth = UsernamePasswordAuthenticationToken(mockCustomUser, null, mockCustomUser.authorities)
        mockMvc.perform(patch("/api/v1/bookbook/users/me")
            .with(authentication(auth))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().is4xxClientError)
            .andExpect(jsonPath("$.resultCode").value("400-NO-CHANGES"))
    }

    @Test
    @DisplayName("비인증 상태에서 정보 수정 시 예외 발생 확인")
    @WithAnonymousUser
    fun testUpdateMyInfoNotAuthenticated() {
        val requestDto = UserUpdateRequestDto("newnickname", "newaddress")
        mockMvc.perform(patch("/api/v1/bookbook/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("유저 프로필 조회 확인")
    @WithMockUser
    fun testGetUserProfile() {
        val auth = UsernamePasswordAuthenticationToken(mockCustomUser, null, mockCustomUser.authorities)
        mockMvc.perform(get("/api/v1/bookbook/users/1")
            .with(authentication(auth)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userId").value(1L))
            .andExpect(jsonPath("$.data.nickname").value("nickname-1"))
    }
}