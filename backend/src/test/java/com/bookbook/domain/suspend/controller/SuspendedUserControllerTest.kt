package com.bookbook.domain.suspend.controller

import com.bookbook.UserSetup
import com.bookbook.domain.suspend.service.SuspendedUserService
import com.bookbook.domain.user.service.UserService
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@TestPropertySource(properties = [
    "ALADIN_API_KEY=test-key" // test 프로파일에서 이와 관련한 오류 발생으로 인해 임의로 추가
])
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithMockUser(roles = ["ADMIN"])
class SuspendedUserControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var suspendedUserService: SuspendedUserService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userSetup: UserSetup

    @BeforeAll
    fun setUp() {
        userSetup.setSuspendedUser()
    }

    @Test
    @DisplayName("유저 정지")
    fun t1() {
        val userId = 2
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

        val user = userService.findById(userId.toLong())
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
    }

    @Test
    @DisplayName("정지된 유저를 다시 정지")
    fun t3() {
        val userId = 4
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
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.resultCode").value("409-1"))
            .andExpect(jsonPath("$.msg").value("이 유저는 이미 정지 중입니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("정지된 멤버를 해제")
    fun t4() {
        val userId = 4

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
    @DisplayName("정지가 해제된 유저를 다시 해제 시도")
    fun t5() {
        val userId = 2

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
    fun t6() {
        val userId = 2

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
    fun t7() {
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
}
