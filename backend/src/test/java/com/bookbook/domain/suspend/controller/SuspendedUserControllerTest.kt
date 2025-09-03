package com.bookbook.domain.suspend.controller

import com.bookbook.TestSetup
import com.bookbook.domain.suspend.service.SuspendedUserService
import com.bookbook.domain.user.service.UserService
import com.bookbook.global.security.CustomOAuth2User
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithMockUser(roles = ["ADMIN"])
@DisplayName("SuspendedUser 통합 테스트")
class SuspendedUserControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var suspendedUserService: SuspendedUserService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var testSetup: TestSetup

    private lateinit var mockUser: CustomOAuth2User

    @BeforeAll
    fun before() {
        mockUser = testSetup.createCustomOAuth2User(
            userService.findById(2L)!! // USER 권한 유저
        )
    }

    @Test
    @DisplayName("유저 정지")
    fun t1() {
        val userId = 2L
        val period = 7

        val suspendAction = mvc
            .perform(
                patch("/api/v1/admin/users/suspend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {
                            "userId": $userId,
                            "reason" : "광고",
                            "period" : $period
                        }
                    """
                )
            )
            .andDo(print())

        val user = userService.findById(userId)
            ?: throw NoSuchElementException("존재하지 않는 유저입니다.")

        suspendAction
            .andExpect(handler().handlerType(SuspendedUserController::class.java))
            .andExpect(handler().methodName("suspendUser"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("user${userId}님을 정지하였습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.userStatus").value("SUSPENDED"))
            .andExpect(jsonPath("$.data.suspendedAt").value(Matchers.startsWith(user.suspendedAt.toString().take(20))))
            .andExpect(jsonPath("$.data.resumedAt").value(Matchers.startsWith(user.resumedAt.toString().take(20))))
    }

    @Test
    @DisplayName("정지된 유저 목록 조회")
    fun t2() {
        val page = 1
        val size = 10

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/users/suspend")
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
            .andDo(print())

        val suspendedUsers = suspendedUserService.getSuspendedHistoryPage(page, size).content

        for (i in suspendedUsers.indices) {
            val user = suspendedUsers[i]

            resultAction
                .andExpect(jsonPath("$.data.content[$i].userId").value(user.userId))
                .andExpect(jsonPath("$.data.content[$i].name").value(user.name))
                .andExpect(jsonPath("$.data.content[$i].reason").value(user.reason))
                .andExpect(jsonPath("$.data.content[$i].suspendedAt").value(Matchers.startsWith(user.suspendedAt.toString().take(20))))
                .andExpect(jsonPath("$.data.content[$i].resumedAt").value(Matchers.startsWith(user.resumedAt.toString().take(20))))
        }

        assertThat(suspendedUsers.size).isEqualTo(2)
    }

    @Test
    @DisplayName("정지된 유저 목록 조회 with userId")
    fun t3() {
        val page = 1
        val size = 10
        val userId = 4L

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/users/suspend")
                    .param("page", page.toString())
                    .param("size", size.toString())
                    .param("userId", userId.toString())
            )
            .andDo(print())

        val suspendedUsers = suspendedUserService.getSuspendedHistoryPage(page, size, userId).content

        for (i in suspendedUsers.indices) {
            val user = suspendedUsers[i]

            resultAction
                .andExpect(jsonPath("$.data.content[$i].userId").value(user.userId))
                .andExpect(jsonPath("$.data.content[$i].name").value(user.name))
                .andExpect(jsonPath("$.data.content[$i].reason").value(user.reason))
                .andExpect(jsonPath("$.data.content[$i].suspendedAt").value(Matchers.startsWith(user.suspendedAt.toString().take(20))))
                .andExpect(jsonPath("$.data.content[$i].resumedAt").value(Matchers.startsWith(user.resumedAt.toString().take(20))))
        }

        assertThat(suspendedUsers.size).isEqualTo(1)
    }

    @Test
    @DisplayName("정지된 유저를 다시 정지")
    fun t4() {
        val userId = 4L
        val period = 7

        val user = userService.findById(userId) ?: throw NoSuchElementException()

        assertThat(user.userStatus.toString()).isEqualTo("SUSPENDED")

        val suspendAction = mvc
            .perform(
                patch("/api/v1/admin/users/suspend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "userId": $userId,
                            "reason" : "광고",
                            "period" : $period
                        }
                    """
                    )
            )
            .andDo(print())

        suspendAction
            .andExpect(handler().handlerType(SuspendedUserController::class.java))
            .andExpect(handler().methodName("suspendUser"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.resultCode").value("409-1"))
            .andExpect(jsonPath("$.msg").value("이 유저는 이미 정지 중입니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("정지된 멤버를 해제")
    fun t5() {
        val userId = 4L

        val suspendAction = mvc
            .perform(
                patch("/api/v1/admin/users/$userId/resume")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())

        suspendAction
            .andExpect(handler().handlerType(SuspendedUserController::class.java))
            .andExpect(handler().methodName("resumeUser"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("user${userId}님의 정지가 해제되었습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.userStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.data.resumedAt").doesNotExist())
            .andExpect(jsonPath("$.data.suspendedAt").doesNotExist())
    }

    @Test
    @DisplayName("정지 상태가 아닌 유저를 다시 해제 시도")
    fun t6() {
        val userId = 2L

        val suspendAction = mvc
            .perform(
                patch("/api/v1/admin/users/$userId/resume")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())

        suspendAction
            .andExpect(handler().handlerType(SuspendedUserController::class.java))
            .andExpect(handler().methodName("resumeUser"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.resultCode").value("409-1"))
            .andExpect(jsonPath("$.msg").value("해당 유저의 정지가 이미 해제되어 있습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("유효하지 않은 요청 1")
    fun t7() {
        val userId = 2L

        val suspendAction = mvc
            .perform(
                patch("/api/v1/admin/users/suspend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "userId": $userId,
                            "period": 7
                        }
                    """
                    )
            )
            .andDo(print())

        suspendAction
            .andExpect(handler().handlerType(SuspendedUserController::class.java))
            .andExpect(handler().methodName("suspendUser"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-1"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("유효하지 않은 요청 2")
    fun t8() {
        val suspendAction = mvc
            .perform(
                patch("/api/v1/admin/users/suspend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "reason" : "테스트"
                        }
                    """
                    )
            )
            .andDo(print())

        suspendAction
            .andExpect(handler().handlerType(SuspendedUserController::class.java))
            .andExpect(handler().methodName("suspendUser"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-1"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("어드민 유저 정지 시도")
    fun t9() {
        val userId = 6L
        val period = 7

        val suspendAction = mvc
            .perform(
                patch("/api/v1/admin/users/suspend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "userId": $userId,
                            "reason" : "광고",
                            "period" : $period
                        }
                    """
                    )
            )
            .andDo(print())

        suspendAction
            .andExpect(handler().handlerType(SuspendedUserController::class.java))
            .andExpect(handler().methodName("suspendUser"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-1"))
            .andExpect(jsonPath("$.msg").value("어드민은 정지시킬 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("권한이 없는 유저가 정지를 실시함")
    fun t10() {
        val userId = 3L
        val period = 7

        val suspendAction = mvc
            .perform(
                patch("/api/v1/admin/users/suspend")
                    .with(oauth2Login().oauth2User(mockUser)) // 일반 유저가 로그인 후 정지 시도
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "userId": $userId,
                            "reason" : "광고",
                            "period" : $period
                        }
                    """
                    )
            )
            .andDo(print())

        suspendAction
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("정지된 유저 목록 조회 - 잘못된 page 인수 조정")
    fun t11() {
        val page = -1
        val size = 10

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/users/suspend")
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
            .andDo(print())

        resultAction
            .andExpect(handler().handlerType(SuspendedUserController::class.java))
            .andExpect(handler().methodName("getAllSuspendedHistory"))
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data").exists())
    }

    @Test
    @DisplayName("정지된 유저 목록 조회 - 잘못된 size 인수 조정")
    fun t12() {
        val page = 1
        val size = -1

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/users/suspend")
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
            .andDo(print())

        resultAction
            .andExpect(handler().handlerType(SuspendedUserController::class.java))
            .andExpect(handler().methodName("getAllSuspendedHistory"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data").exists())
    }
}
