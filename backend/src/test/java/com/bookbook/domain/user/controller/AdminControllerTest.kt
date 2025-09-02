package com.bookbook.domain.user.controller

import com.bookbook.TestSetup
import com.bookbook.domain.user.dto.response.UserSimpleResponseDto
import com.bookbook.domain.user.enums.UserStatus
import com.bookbook.domain.user.service.AdminService
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
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithMockUser(roles = ["ADMIN"])
@DisplayName("AdminController 통합 테스트")
class AdminControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var adminService: AdminService

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
    @DisplayName("로그인")
    fun t1() {
        val username = "admin-test"
        val password = "admin-test-pass"
        val nickname = "admin-test-nick"

        val resultActions = mvc
            .perform(
                post("/api/v1/admin/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "username": "$username",
                            "password": "$password"
                        }
                        """
                    )
            )
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(AdminController::class.java))
            .andExpect(handler().methodName("adminLogin"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("관리자 ${username}님이 로그인하였습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.username").value(username))
            .andExpect(jsonPath("$.data.nickname").value(nickname))
            .andExpect(jsonPath("$.data.role").value("ADMIN"))
            .andExpect { result ->
                val accessToken = result.response.getCookie("JWT_TOKEN") ?: throw NoSuchElementException()
                val refreshToken = result.response.getCookie("refreshToken") ?: throw NoSuchElementException()

                assertThat(accessToken.value).isNotBlank
                assertThat(accessToken.path).isEqualTo("/")
                assertThat(accessToken.isHttpOnly).isTrue

                assertThat(refreshToken.value).isNotBlank
                assertThat(refreshToken.path).isEqualTo("/")
                assertThat(refreshToken.isHttpOnly).isTrue
            }
    }

    @Test
    @DisplayName("유저가 로그인 시도")
    fun t2() {
        val username = "user1"
        val password = "password-1"

        val resultActions = mvc
            .perform(
                post("/api/v1/admin/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "username": "$username",
                            "password": "$password"
                        }
                        """
                    )
            )
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(AdminController::class.java))
            .andExpect(handler().methodName("adminLogin"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.resultCode").value("401-UNAUTHORIZED"))
            .andExpect(jsonPath("$.msg").value("허가되지 않은 접근입니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("로그아웃")
    fun t3() {
        val resultActions = mvc
            .perform(
                delete("/api/v1/admin/logout")
            )
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(AdminController::class.java))
            .andExpect(handler().methodName("adminLogout"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("로그아웃을 정상적으로 완료했습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect { result ->
                val accessToken = result.response.getCookie("JWT_TOKEN") ?: throw NoSuchElementException()
                assertThat(accessToken.value).isNullOrEmpty()
                assertThat(accessToken.maxAge).isEqualTo(0)
                assertThat(accessToken.path).isEqualTo("/")
                assertThat(accessToken.isHttpOnly).isTrue

                val refreshToken = result.response.getCookie("refreshToken") ?: throw NoSuchElementException()
                assertThat(refreshToken.value).isNullOrEmpty()
                assertThat(refreshToken.maxAge).isEqualTo(0)
                assertThat(refreshToken.path).isEqualTo("/")
                assertThat(refreshToken.isHttpOnly).isTrue
            }
    }

    @Test
    @DisplayName("유저 세부 정보 가져오기")
    fun t4() {
        val userId = 4L

        val resultActions = mvc
            .perform(
                get("/api/v1/admin/users/$userId")
            )
            .andDo(print())

        val user = userService.findById(userId) ?: throw NoSuchElementException()

        resultActions
            .andExpect(handler().handlerType(AdminController::class.java))
            .andExpect(handler().methodName("getUserDetail"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("유저 \"${user.nickname}\"님의 정보를 찾았습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.id").value(user.id))
            .andExpect(jsonPath("$.data.username").value(user.username))
            .andExpect(jsonPath("$.data.nickname").value(user.nickname))
            .andExpect(jsonPath("$.data.rating").value(user.rating))
            .andExpect(
                jsonPath("$.data.createdAt")
                    .value(Matchers.startsWith(user.createdDate.toString().take(20)))
            )
            .andExpect(
                jsonPath("$.data.updatedAt")
                    .value(Matchers.startsWith(user.modifiedDate.toString().take(20)))
            )
    }

    @Test
    @DisplayName("유저 목록 가져오기 without filters")
    fun t5() {
        val page = 1
        val size = 10

        val resultActions = mvc
            .perform(
                get("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "page" : $page,
                      "size" : $size
                    }
                  """
                )

            )
            .andDo(print())

        val pageable = PageRequest.of(page - 1, size)

        val pagedUsers = adminService.getFilteredUsers(pageable, null, null)
        val users = pagedUsers.content

        resultActions
            .andExpect(handler().handlerType(AdminController::class.java))
            .andExpect(handler().methodName("getFilteredUsers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("해당 조건에 맞는 ${pagedUsers.totalElements}명의 유저를 찾았습니다."))
            .andExpect(jsonPath("$.data").exists())

        iterateFromList(resultActions, users)
    }

    @Test
    @DisplayName("유저 목록 가져오기 with SUSPENDED status")
    fun t6() {
        val page = 1
        val size = 10
        val statuses = listOf(UserStatus.SUSPENDED)

        val resultActions = mvc
            .perform(
                get("/api/v1/admin/users")
                .param("page", page.toString())
                .param("size", size.toString())
                .apply {
                    statuses.forEach { status ->
                        param("status", status.toString())
                    }
                }
            )
            .andDo(print())

        val pageable = PageRequest.of(page - 1, size)

        val pagedUsers = adminService.getFilteredUsers(pageable, statuses, null)
        val users = pagedUsers.content

        resultActions
            .andExpect(handler().handlerType(AdminController::class.java))
            .andExpect(handler().methodName("getFilteredUsers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("해당 조건에 맞는 2명의 유저를 찾았습니다."))
            .andExpect(jsonPath("$.data").exists())

        iterateFromList(resultActions, users)
    }

    @Test
    @DisplayName("유저 목록 가져오기 with SUSPENDED, INACTIVE status")
    fun t7() {
        val page = 1
        val size = 10
        val statuses = listOf(UserStatus.SUSPENDED, UserStatus.INACTIVE)

        val resultActions = mvc
            .perform(
                get("/api/v1/admin/users")
                .param("page", page.toString())
                .param("size", size.toString())
                .apply {
                    statuses.forEach { status ->
                        param("status", status.toString())
                    }
                }
            )
            .andDo(print())

        val pageable = PageRequest.of(page - 1, size)

        val pagedUsers = adminService.getFilteredUsers(pageable, statuses, null)
        val users = pagedUsers.content

        resultActions
            .andExpect(handler().handlerType(AdminController::class.java))
            .andExpect(handler().methodName("getFilteredUsers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("해당 조건에 맞는 2명의 유저를 찾았습니다."))
            .andExpect(jsonPath("$.data").exists())

        iterateFromList(resultActions, users)
    }

    @Test
    @DisplayName("유저 목록 가져오기 with single userId")
    fun t8() {
        val page = 1
        val size = 10
        val userId = 5L

        val resultActions = mvc
            .perform(
                get("/api/v1/admin/users")
                    .param("page", page.toString())
                    .param("size", size.toString())
                    .param("userId", userId.toString())
            )
            .andDo(print())

        val pageable = PageRequest.of(page - 1, size)

        val pagedUsers = adminService.getFilteredUsers(pageable, null, userId)
        val users = pagedUsers.content

        resultActions
            .andExpect(handler().handlerType(AdminController::class.java))
            .andExpect(handler().methodName("getFilteredUsers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("해당 조건에 맞는 1명의 유저를 찾았습니다."))
            .andExpect(jsonPath("$.data").exists())

        iterateFromList(resultActions, users)
    }

    @Test
    @DisplayName("유저 목록 가져오기 with lower size")
    fun t9() {
        val page = 1
        val size = 5

        val resultActions = mvc
            .perform(
                get("/api/v1/admin/users")
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
            .andDo(print())

        val pageable = PageRequest.of(page - 1, size)

        val pagedUsers = adminService.getFilteredUsers(pageable, null, null)
        val users = pagedUsers.content

        resultActions
            .andExpect(handler().handlerType(AdminController::class.java))
            .andExpect(handler().methodName("getFilteredUsers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("해당 조건에 맞는 7명의 유저를 찾았습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.pageInfo").exists())
            .andExpect(jsonPath("$.data.pageInfo.currentPageElements").value(users.size))

        assertThat(users.size).isEqualTo(size)

        iterateFromList(resultActions, users)
    }

    @Test
    @DisplayName("유저 목록 조회 - 잘못된 page 인수")
    fun t10() {
        val page = -1
        val size = 10

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/users")
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
            .andDo(print())

        resultAction
            .andExpect(handler().handlerType(AdminController::class.java))
            .andExpect(handler().methodName("getFilteredUsers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data").exists())

    }

    @Test
    @DisplayName("유저 목록 조회 - 잘못된 size 인수")
    fun t11() {
        val page = 1
        val size = -1

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/users")
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
            .andDo(print())

        resultAction
            .andExpect(handler().handlerType(AdminController::class.java))
            .andExpect(handler().methodName("getFilteredUsers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data").exists())

    }

    private fun iterateFromList(resultActions: ResultActions, users: List<UserSimpleResponseDto>) {
        for (i in users.indices) {
            val user = users[i]

            resultActions
                .andExpect(jsonPath("$.data.content[$i].id").value(user.id))
                .andExpect(
                    jsonPath("$.data.content[$i].createdAt")
                        .value(Matchers.startsWith(user.createdAt.toString().take(20)))
                )
                .andExpect(
                    jsonPath("$.data.content[$i].updatedAt")
                        .value(Matchers.startsWith(user.updatedAt.toString().take(20)))
                )
                .andExpect(jsonPath("$.data.content[$i].nickname").value(user.nickname))
                .andExpect(jsonPath("$.data.content[$i].username").value(user.username))
        }
    }
}