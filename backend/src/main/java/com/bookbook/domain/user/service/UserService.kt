package com.bookbook.domain.user.service

import com.bookbook.domain.review.repository.ReviewRepository
import com.bookbook.domain.user.dto.response.UserProfileResponseDto
import com.bookbook.domain.user.dto.response.UserResponseDto
import com.bookbook.domain.user.dto.response.UserStatusResponseDto
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.enums.Role
import com.bookbook.domain.user.enums.UserStatus
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.exception.ServiceException
import jakarta.annotation.PostConstruct
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jdbcTemplate: JdbcTemplate,
    private val reviewRepository: ReviewRepository
) {

    @PostConstruct
    @Transactional
    fun initializeData() {
        createAdminUser()
        updateNotificationSchema()
    }

    private fun createAdminUser() {
        val adminUsername = "admin"
        val adminPassword = "admin123"
        val adminEmail = "admin@book.com"

        if (!userRepository.existsByUsername(adminUsername)) {
            val adminUser = User(
                username = adminUsername,
                password = passwordEncoder.encode(adminPassword),
                email = adminEmail,
                nickname = "관리자",
                address = "서울시 강남구",
                role = Role.ADMIN,
                registrationCompleted = true
            )
            userRepository.save(adminUser)
            println("관리자 계정이 생성되었습니다: $adminUsername")
        } else {
            println("관리자 계정이 이미 존재합니다: $adminUsername")
        }
    }

    private fun updateNotificationSchema() {
        try {
            jdbcTemplate.execute("ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check")
            jdbcTemplate.execute("ALTER TABLE notification ADD CONSTRAINT notification_type_check CHECK (type IN ('RENT_REQUEST', 'RENT_APPROVED', 'RENT_REJECTED', 'WISHLIST_AVAILABLE', 'RETURN_REMINDER', 'POST_CREATED'))")
            println("✅ NotificationType 스키마 업데이트 완료: 모든 enum 값 포함됨")
        } catch (e: Exception) {
            System.err.println("❌ NotificationType 스키마 업데이트 실패: ${e.message}")
        }
    }

    fun checkNicknameAvailability(nickname: String): Boolean {
        return !userRepository.existsByNickname(nickname)
    }

    @Transactional
    fun completeRegistration(userId: Long, nickname: String?, address: String?) {
        val user = getByIdOrThrow(userId)

        val trimmedNickname = nickname?.trim()
        val trimmedAddress = address?.trim()

        if (trimmedNickname.isNullOrEmpty()) {
            throw ServiceException("400-NICKNAME-EMPTY", "닉네임은 필수 입력 사항입니다.")
        }
        if (userRepository.existsByNickname(trimmedNickname)) {
            throw ServiceException("409-NICKNAME-DUPLICATE", "이미 사용 중인 닉네임입니다.")
        }
        if (trimmedAddress.isNullOrEmpty()) {
            throw ServiceException("400-ADDRESS-EMPTY", "주소는 필수 입력 사항입니다.")
        }

        user.nickname = trimmedNickname
        user.address = trimmedAddress
        user.registrationCompleted = true
        userRepository.save(user)
    }

    @Transactional
    fun updateUserInfo(userId: Long, nickname: String?, address: String?) {
        val user = getByIdOrThrow(userId)

        val trimmedNickname = nickname?.trim()
        val trimmedAddress = address?.trim()

        var hasChanges = false

        if (!trimmedNickname.isNullOrEmpty() && trimmedNickname != user.nickname) {
            if (userRepository.existsByNickname(trimmedNickname)) {
                throw ServiceException("409-NICKNAME-DUPLICATE", "이미 사용 중인 닉네임입니다.")
            }
            user.nickname = trimmedNickname
            hasChanges = true
        }

        if (!trimmedAddress.isNullOrEmpty() && trimmedAddress != user.address) {
            user.address = trimmedAddress
            hasChanges = true
        }

        if (!hasChanges) {
            throw ServiceException("400-NO-CHANGES", "변경할 닉네임 또는 주소를 제공하지 않았거나 변경사항이 없습니다.")
        }

        user.registrationCompleted = true
        userRepository.save(user)
    }

    fun findById(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }

    @Transactional
    fun deactivateUser(userId: Long) {
        val user = getByIdOrThrow(userId)

        if (user.userStatus == UserStatus.INACTIVE) {
            throw ServiceException("409-USER-ALREADY-INACTIVE", "이미 탈퇴 처리된 사용자입니다.")
        }

        user.apply {
            userStatus = UserStatus.INACTIVE
            username = "${username}_deleted_${UUID.randomUUID()}"
            email = email?.let { "${it}_deleted_${UUID.randomUUID()}" }
            nickname = null
            address = null
            registrationCompleted = false
        }

        userRepository.save(user)
    }

    fun getUserDetails(userId: Long): UserResponseDto {
        val user = getByIdOrThrow(userId)
        return UserResponseDto(user)
    }

    fun getUserStatus(userId: Long): UserStatusResponseDto {
        val user = getByIdOrThrow(userId)
        return UserStatusResponseDto(user.id, user.userStatus)
    }

    @Transactional
    fun findOrCreateUser(username: String, email: String?, socialNickname: String?): User {
        return userRepository.findByUsername(username)
            .orElseGet {
                val newUser = User(
                    username = username,
                    email = email,
                    password = passwordEncoder.encode(UUID.randomUUID().toString()),
                )
                userRepository.save(newUser)
                newUser
            }
    }

    fun getByIdOrThrow(id: Long): User {
        return userRepository.findById(id)
            .orElseThrow { ServiceException("404-USER-NOT-FOUND", "해당 사용자를 찾을 수 없습니다.") }
    }

    fun getUserProfileDetails(userId: Long): UserProfileResponseDto {
        val user = getByIdOrThrow(userId)

        return if (user.isAdmin) {
            UserProfileResponseDto(user, 5.0, 0)
        } else {
            val averageRating = reviewRepository.findAverageRatingByRevieweeId(userId).orElse(0.0)
            val mannerScoreCount = reviewRepository.countByRevieweeId(userId)
            UserProfileResponseDto(user, averageRating, mannerScoreCount.toInt())
        }
    }
}