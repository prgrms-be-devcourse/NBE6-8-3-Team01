package com.bookbook.domain.user.controller

import com.bookbook.domain.user.dto.request.UserCreateRequestDto
import com.bookbook.domain.user.dto.request.UserUpdateRequestDto
import com.bookbook.domain.user.dto.response.UserProfileResponseDto
import com.bookbook.domain.user.dto.response.UserResponseDto
import com.bookbook.domain.user.dto.response.UserStatusResponseDto
import com.bookbook.domain.user.service.UserService
import com.bookbook.global.exception.ServiceException
import com.bookbook.global.rsdata.RsData
import com.bookbook.global.security.CustomOAuth2User
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/bookbook/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/check-nickname")
    fun checkNickname(@RequestParam nickname: String): ResponseEntity<RsData<Map<String, Boolean>>> {
        if (nickname.trim().isEmpty()) {
            throw ServiceException("400-NICKNAME-EMPTY", "닉네임을 입력해주세요.")
        }
        val isAvailable = userService.checkNicknameAvailability(nickname)
        val responseData = mapOf("isAvailable" to isAvailable)
        val rsData = RsData("200-OK", "닉네임 중복 확인 완료", responseData)
        return ResponseEntity.status(rsData.statusCode).body(rsData)
    }

    @PostMapping("/signup")
    fun completeSignup(
        @Validated @RequestBody signupRequest: UserCreateRequestDto,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): ResponseEntity<RsData<Void>> {
        val userId = customOAuth2User?.userId
            ?: throw ServiceException("401-AUTH-INVALID", "로그인 정보가 유효하지 않습니다.")
        if (userId == -1L) {
            throw ServiceException("401-AUTH-INVALID", "로그인 정보가 유효하지 않습니다.")
        }
        userService.completeRegistration(userId, signupRequest.nickname, signupRequest.address)
        val rsData = RsData<Void>(resultCode = "200-OK", msg = "회원가입이 완료되었습니다.", data = null)
        return ResponseEntity.status(rsData.statusCode).body(rsData)
    }

    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal customOAuth2User: CustomOAuth2User?): ResponseEntity<RsData<UserResponseDto>> {
        val userId = customOAuth2User?.userId
            ?: throw ServiceException("401-AUTH-INVALID", "로그인된 사용자가 없습니다.")
        val userDetails = userService.getUserDetails(userId)
        val rsData = RsData("200-OK", "사용자 정보 조회 성공", userDetails)
        return ResponseEntity.status(rsData.statusCode).body(rsData)
    }

    @GetMapping("/status")
    fun getCurrentUserStatus(@AuthenticationPrincipal customOAuth2User: CustomOAuth2User?): ResponseEntity<RsData<UserStatusResponseDto>> {
        val userId = customOAuth2User?.userId
            ?: throw ServiceException("401-AUTH-INVALID", "로그인된 사용자가 없습니다.")
        val userDetails = userService.getUserStatus(userId)
        val rsData = RsData("200-OK", "사용자 정보 조회 성공", userDetails)
        return ResponseEntity.status(rsData.statusCode).body(rsData)
    }

    @GetMapping("/isAuthenticated")
    fun isAuthenticated(@AuthenticationPrincipal customOAuth2User: CustomOAuth2User?): ResponseEntity<RsData<Boolean>> {
        val authenticated = customOAuth2User?.userId != null && customOAuth2User.userId != -1L
        val rsData = RsData("200-OK", "인증 여부 확인 완료", authenticated)
        return ResponseEntity.status(rsData.statusCode).body(rsData)
    }

    @DeleteMapping("/me")
    fun deactivateUser(@AuthenticationPrincipal customOAuth2User: CustomOAuth2User?): ResponseEntity<RsData<Void>> {
        val userId = customOAuth2User?.userId
            ?: throw ServiceException("401-AUTH-INVALID", "로그인 정보가 유효하지 않습니다.")
        userService.deactivateUser(userId)
        val rsData = RsData<Void>(resultCode = "200-OK", msg = "회원 탈퇴가 성공적으로 처리되었습니다.", data = null)
        return ResponseEntity.status(rsData.statusCode).body(rsData)
    }

    @PatchMapping("/me")
    fun updateMyInfo(
        @Valid @RequestBody updateRequest: UserUpdateRequestDto,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): ResponseEntity<RsData<Void>> {
        val userId = customOAuth2User?.userId
            ?: throw ServiceException("401-AUTH-INVALID", "로그인 정보가 유효하지 않습니다.")
        if (userId == -1L) {
            throw ServiceException("401-AUTH-INVALID", "로그인 정보가 유효하지 않습니다.")
        }
        val nickname = updateRequest.nickname?.trim()
        val address = updateRequest.address?.trim()
        if (nickname.isNullOrEmpty() && address.isNullOrEmpty()) {
            throw ServiceException("400-NO-CHANGES", "수정할 닉네임 또는 주소를 제공해야 합니다.")
        }
        userService.updateUserInfo(userId, updateRequest.nickname, updateRequest.address)
        val rsData = RsData<Void>(resultCode = "200-OK", msg = "회원 정보가 성공적으로 수정되었습니다.", data = null)
        return ResponseEntity.status(rsData.statusCode).body(rsData)
    }

    @GetMapping("/{userId}")
    fun getUserProfile(@PathVariable userId: Long): ResponseEntity<RsData<UserProfileResponseDto>> {
        val userProfile = userService.getUserProfileDetails(userId)
        val rsData = RsData("200-OK", "사용자 프로필 조회 성공", userProfile)
        return ResponseEntity.status(rsData.statusCode).body(rsData)
    }
}