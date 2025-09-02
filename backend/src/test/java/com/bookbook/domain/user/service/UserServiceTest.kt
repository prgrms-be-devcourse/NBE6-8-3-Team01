package com.bookbook.domain.user.service

import com.bookbook.TestSetup
import com.bookbook.domain.review.repository.ReviewRepository
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.enums.UserStatus
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.exception.ServiceException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("UserService 통합 테스트")
class UserServiceTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var testSetup: TestSetup

    private val passwordEncoder: PasswordEncoder = mock(PasswordEncoder::class.java)
    private val reviewRepository: ReviewRepository = mock(ReviewRepository::class.java)

    private lateinit var savedUser: User

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder)
        ReflectionTestUtils.setField(userService, "reviewRepository", reviewRepository)

        testSetup.createDummyUser()
        savedUser = userRepository.findByUsername("user1").get()
    }

    @Test
    @DisplayName("유저 ID로 유저 상세정보를 성공적으로 조회하는지 확인")
    fun getUserDetails_validId_success() {
        val result = userService.getUserDetails(savedUser.id)

        assertNotNull(result)
        assertEquals(savedUser.id, result.id)
        assertEquals(savedUser.username, result.username)
        assertEquals(savedUser.nickname, result.nickname)
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID로 유저 조회 시 예외 발생 확인")
    fun getUserStatus_invalidId_throwsException() {
        val userId = 999L

        assertThrows(ServiceException::class.java) {
            userService.getUserStatus(userId)
        }
    }

    @Test
    @DisplayName("유저 상태를 성공적으로 조회하는지 확인")
    fun getUserStatus_validId_success() {
        val result = userService.getUserStatus(savedUser.id)

        assertNotNull(result)
        assertEquals(savedUser.id, result.id)
        assertEquals(savedUser.userStatus, result.userStatus)
    }

    @Test
    @DisplayName("새로운 유저를 성공적으로 생성하는지 확인")
    fun findOrCreateUser_newUser_createsAndReturns() {
        val newUsername = "new_test_user"
        val newEmail = "new@example.com"
        val socialNickname = "social_nick"
        `when`(passwordEncoder.encode(anyString())).thenReturn("hashed_password")
        val initialUserCount = userRepository.count()

        val createdUser = userService.findOrCreateUser(newUsername, newEmail, socialNickname)

        assertNotNull(createdUser)
        assertEquals(newUsername, createdUser.username)
        val afterUserCount = userRepository.count()
        assertEquals(initialUserCount + 1, afterUserCount)
        val foundUser = userRepository.findByUsername(newUsername).orElse(null)
        assertNotNull(foundUser)
        assertEquals(newEmail, foundUser.email)
    }

    @Test
    @DisplayName("기존 유저를 성공적으로 찾아 반환하는지 확인")
    fun findOrCreateUser_existingUser_returnsExisting() {
        val initialUserCount = userRepository.count()

        val foundUser = userService.findOrCreateUser(savedUser.username, null, null)

        assertNotNull(foundUser)
        assertEquals(savedUser.username, foundUser.username)
        assertEquals(initialUserCount, userRepository.count())
    }

    @Test
    @DisplayName("유저 프로필 상세 정보를 성공적으로 조회하는지 확인 (일반 유저)")
    fun getUserProfile_regularUser_success() {
        val mannerScore = 4.5
        val mannerScoreCount = 10L
        `when`(reviewRepository.findAverageRatingByRevieweeId(anyLong())).thenReturn(Optional.of(mannerScore))
        `when`(reviewRepository.countByRevieweeId(anyLong())).thenReturn(mannerScoreCount)

        val result = userService.getUserProfileDetails(savedUser.id)

        assertNotNull(result)
        assertEquals(savedUser.id, result.userId)
        assertEquals(savedUser.nickname, result.nickname)
        assertEquals(mannerScore, result.mannerScore, 0.0)
        assertEquals(mannerScoreCount.toInt(), result.mannerScoreCount)
        verify(reviewRepository).findAverageRatingByRevieweeId(savedUser.id)
        verify(reviewRepository).countByRevieweeId(savedUser.id)
    }

    @Test
    @DisplayName("유저 등록을 성공적으로 완료하는지 확인")
    fun completeRegistration_success() {
        val userToRegister = userRepository.save(User(username = "reg_test_user", password = "test_password"))
        val nickname = "reg_nick"
        val address = "test_address"

        userService.completeRegistration(userToRegister.id, nickname, address)

        val updatedUser = userRepository.findById(userToRegister.id).get()
        assertEquals(nickname, updatedUser.nickname)
        assertEquals(address, updatedUser.address)
        assertTrue(updatedUser.registrationCompleted)
    }

    @Test
    @DisplayName("닉네임 중복 시 회원정보 업데이트 실패 확인")
    fun updateUserInfo_duplicateNickname_throwsException() {
        val userToUpdate = savedUser
        val otherUser = userRepository.findByUsername("user2").get()
        val duplicateNickname = otherUser.nickname

        assertThrows(ServiceException::class.java) {
            userService.updateUserInfo(userToUpdate.id, duplicateNickname, "new_address")
        }
    }

    @Test
    @DisplayName("유저를 성공적으로 비활성화하는지 확인")
    fun deactivateUser_success() {
        val userToDeactivate = userRepository.save(User(username = "deact_test", email = "deact@test.com", password = "test_password"))

        userService.deactivateUser(userToDeactivate.id)

        val deactivatedUser = userRepository.findById(userToDeactivate.id).get()
        assertEquals(UserStatus.INACTIVE, deactivatedUser.userStatus)
        assertNull(deactivatedUser.nickname)
        assertTrue(deactivatedUser.username.startsWith("deact_test_deleted_"))
    }
}