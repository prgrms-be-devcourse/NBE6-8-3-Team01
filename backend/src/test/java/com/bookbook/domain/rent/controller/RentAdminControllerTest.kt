package com.bookbook.domain.rent.controller

import com.bookbook.TestSetup
import com.bookbook.domain.rent.dto.request.RentRequestDto
import com.bookbook.domain.rent.dto.response.RentSimpleResponseDto
import com.bookbook.domain.rent.entity.RentStatus
import com.bookbook.domain.rent.service.RentService
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
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
@DisplayName("RentAdminController 통합 테스트")
class RentAdminControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var rentService: RentService

    @Autowired
    private lateinit var testSetup: TestSetup

    @Autowired
    private lateinit var userService: UserService

    private val conditions = listOf(
        "최상 (깨끗함)",
        "상 (사용감 적음)",
        "중 (사용감 있음)",
        "하 (손상 있음)"
    )

    private val rentStatuses = RentStatus.entries

    private lateinit var mockUser: CustomOAuth2User

    @BeforeAll
    fun setUp() {
        mockUser = testSetup.createCustomOAuth2User(
            userService.findById(2L)!! // USER 권한 유저
        )

        // 게시글 더미데이터 추가
        for (i in 1..20) {
            val id = (i - 1) % conditions.size + 1
            val userId = (i - 1) % 4 + 2
            val statusIndex = (i - 1) % rentStatuses.size

            rentService.createRentPage(
                RentRequestDto(
                    title = "도서 $i 읽어보실 분",
                    bookCondition = conditions[id - 1],
                    bookImage = "https://images.pexels.com/photos/762687/pexels-photo-762687.jpeg",
                    address = "서울시",
                    contents = "이 도서 $i 를 읽어보세요. 꽤 재밌습니다",
                    rentStatus = rentStatuses[statusIndex],
                    bookTitle = "도서 $i",
                    author = "저자 $i",
                    publisher = "출판사 $i",
                    category = "카테고리 $i",
                    description = "책$i 설명"
                ),
                userId.toLong()
            )
        }
    }

    @Test
    @DisplayName("대여 게시글 목록 조회")
    fun t1() {
        val page = 1
        val size = 10

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/rent")
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
            .andDo(print())

        val pageable = PageRequest.of(page - 1, size)
        val posts = rentService.getRentsPage(pageable, null, null).content

        assertThat(posts.size).isEqualTo(size)

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("getPosts"))
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("33개의 글을 발견했습니다."))
            .andExpect(jsonPath("$.data").exists())

        iteratePostDto(resultAction, posts)
    }

    @Test
    @DisplayName("대여 게시글 목록 조회 - 4페이지")
    fun t2() {
        val page = 4
        val size = 10

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/rent")
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
            .andDo(print())

        val pageable = PageRequest.of(page - 1, size)
        val posts = rentService.getRentsPage(pageable, null, null).content

        assertThat(posts.size).isEqualTo(3)

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("getPosts"))
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("33개의 글을 발견했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.pageInfo.currentPage").value(page))

        iteratePostDto(resultAction, posts)
    }

    @Test
    @DisplayName("대여 게시글 목록 조회 - 페이지 5, 사이즈 8")
    fun t3() {
        val page = 5
        val size = 8

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/rent")
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
            .andDo(print())

        val pageable = PageRequest.of(page - 1, size)
        val posts = rentService.getRentsPage(pageable, null, null).content

        assertThat(posts.size).isEqualTo(1)

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("getPosts"))
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("33개의 글을 발견했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.pageInfo.currentPage").value(page))

        iteratePostDto(resultAction, posts)
    }

    @Test
    @DisplayName("대여 게시글 목록 조회 - 잘못된 page 인수")
    fun t4() {
        val page = -1
        val size = 10

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/rent")
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
            .andDo(print())

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("getPosts"))
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("33개의 글을 발견했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.pageInfo.currentPage").value(1))
    }

    @Test
    @DisplayName("대여 게시글 목록 조회 - 잘못된 size 인수")
    fun t5() {
        val page = 1
        val size = -1

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/rent")
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
            .andDo(print())

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("getPosts"))
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("33개의 글을 발견했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.pageInfo.size").value(10))
    }

    @Test
    @DisplayName("대여 게시글 목록 조회 - 초과된 page")
    fun t6() {
        val page = 5
        val size = 10

        // 데이터 33개, 페이지 당 10개 가정
        // 1p: 10, 2p: 10, 3p: 10, 4p 3, 5p X
        val resultAction = mvc
            .perform(
                get("/api/v1/admin/rent")
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
            .andDo(print())

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("getPosts"))
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("33개의 글을 발견했습니다."))
            .andExpect(jsonPath("$.data").exists())
    }

    @Test
    @DisplayName("대여 게시글 목록 조회 - 글 상태로 검색")
    fun t7() {
        val page = 1
        val size = 10
        val statuses = listOf(RentStatus.LOANED)

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/rent")
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
        val posts = rentService.getRentsPage(pageable, statuses, null).content

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("getPosts"))
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("5개의 글을 발견했습니다."))
            .andExpect(jsonPath("$.data").exists())

        for (i in posts.indices) {
            resultAction
                .andExpect(jsonPath("$.data.content[$i].status").value(RentStatus.LOANED.toString()))
        }

    }

    @Test
    @DisplayName("대여 게시글 목록 조회 - 작성자 ID로 검색")
    fun t8() {
        val page = 1
        val size = 10
        val userId = 4L

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/rent")
                    .param("page", page.toString())
                    .param("size", size.toString())
                    .param("userId", userId.toString())
            )
            .andDo(print())

        val pageable = PageRequest.of(page - 1, size)
        val posts = rentService.getRentsPage(pageable, null, userId).content

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("getPosts"))
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("5개의 글을 발견했습니다."))
            .andExpect(jsonPath("$.data").exists())

        for (i in posts.indices) {
            resultAction
                .andExpect(jsonPath("$.data.content[$i].lenderUserId").value(userId))
        }
    }

    @Test
    @DisplayName("대여 게시글 상세 조회")
    fun t9() {
        val id = 1L

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/rent/$id")
            )
            .andDo(print())

        val postDto = rentService.getRentPostDetail(id)

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("getRentDetail"))
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("${id}번 글 조회 완료."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.id").value(id))
            .andExpect(jsonPath("$.data.lenderUserId").value(postDto.lenderUserId))
            .andExpect(jsonPath("$.data.title").value(postDto.title))
            .andExpect(jsonPath("$.data.bookCondition").value(postDto.bookCondition))
            .andExpect(jsonPath("$.data.bookImage").value(postDto.bookImage))
            .andExpect(jsonPath("$.data.address").value(postDto.address))
            .andExpect(jsonPath("$.data.contents").value(postDto.contents))
            .andExpect(jsonPath("$.data.rentStatus").value(postDto.rentStatus.toString()))
            .andExpect(jsonPath("$.data.bookTitle").value(postDto.bookTitle))
            .andExpect(jsonPath("$.data.author").value(postDto.author))
            .andExpect(jsonPath("$.data.publisher").value(postDto.publisher))
            .andExpect(jsonPath("$.data.category").value(postDto.category))
            .andExpect(jsonPath("$.data.description").value(postDto.description))
            .andExpect(
                jsonPath("$.data.createdDate").value(
                    Matchers.startsWith(
                        postDto.createdDate.toString().take(20)
                    )
                )
            )
            .andExpect(
                jsonPath("$.data.modifiedDate").value(
                    Matchers.startsWith(
                        postDto.modifiedDate.toString().take(20)
                    )
                )
            )
    }

    @Test
    @DisplayName("대여 게시글 상세 조회 실패")
    fun t10() {
        val id = 99L

        val resultAction = mvc
            .perform(
                get("/api/v1/admin/rent/$id")
            )
            .andDo(print())

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("getRentDetail"))
            .andExpect(jsonPath("$.resultCode").value("404-2"))
            .andExpect(jsonPath("$.msg").value("해당 대여글을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("대여 게시글 상태 변경 - 성공")
    fun t11() {
        val id = 1L
        val requestedStatus = RentStatus.FINISHED.toString()

        val resultAction = mvc
            .perform(
                patch("/api/v1/admin/rent/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "status" : "$requestedStatus"
                        }
                    """)
            )
            .andDo(print())

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("changeRentStatus"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("${id}번 글 상태 변경 완료."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.rentStatus").value(requestedStatus))
    }

    @Test
    @DisplayName("대여 게시글 상태 변경 - 동일한 상태로 수정 시도 (실패)")
    fun t12() {
        val id = 1L
        val requestedStatus = RentStatus.FINISHED.toString()

        fun request(): ResultActions {
            return mvc.perform(
                patch("/api/v1/admin/rent/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "status" : "$requestedStatus"
                        }
                    """
                )
            )
        }

        request()
        val resultAction = request().andDo(print())

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("changeRentStatus"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.resultCode").value("409-1"))
            .andExpect(jsonPath("$.msg").value("현재 상태와 동일합니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("존재하지 않는 ID로 수정 시도")
    fun t13() {
        val id = 99L
        val requestedStatus = RentStatus.FINISHED.toString()

        val resultAction = mvc
            .perform(
                patch("/api/v1/admin/rent/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "status" : "$requestedStatus"
                        }
                    """)
            )
            .andDo(print())

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("changeRentStatus"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-2"))
            .andExpect(jsonPath("$.msg").value("해당 대여글을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("삭제된 대여 게시글 복구 - 성공")
    fun t14() {
        val id = 17L

        val resultAction = mvc
            .perform(
                patch("/api/v1/admin/rent/${id}/restore")
            )
            .andDo(print())

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("restoreRentPage"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("${id}번 글 복구 완료."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.rentStatus").value("AVAILABLE"))
    }

    @Test
    @DisplayName("삭제 상태가 아닌 글 복구 - 실패")
    fun t15() {
        val id = 17L

        // 복구 시도
        mvc.perform(
            patch("/api/v1/admin/rent/${id}/restore")
        )

        // 정상 상태로 돌아온 글을 다시 복구 시도
        val resultAction = mvc.perform(
            patch("/api/v1/admin/rent/${id}/restore")
            )
            .andDo(print())

        resultAction
            .andExpect(handler().handlerType(RentAdminController::class.java))
            .andExpect(handler().methodName("restoreRentPage"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.resultCode").value("409-1"))
            .andExpect(jsonPath("$.msg").value("해당 글은 삭제된 상태가 아닙니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("권한이 없는 유저가 대여 기록을 가져오기를 시도")
    fun t16() {
        val suspendAction = mvc
            .perform(
                get("/api/v1/admin/rent")
                    .with(oauth2Login().oauth2User(mockUser)) // 일반 유저가 로그인 후 정지 시도
            )
            .andDo(print())

        suspendAction
            .andExpect(status().isForbidden)
    }

    private fun iteratePostDto(resultAction: ResultActions, posts: List<RentSimpleResponseDto>) {
        for (i in posts.indices) {
            val post = posts[i]

            resultAction
                .andExpect(jsonPath("$.data.content[$i].id").value(post.id))
                .andExpect(jsonPath("$.data.content[$i].lenderUserId").value(post.lenderUserId))
                .andExpect(jsonPath("$.data.content[$i].status").value(post.status.toString()))
                .andExpect(jsonPath("$.data.content[$i].bookCondition").value(post.bookCondition))
                .andExpect(jsonPath("$.data.content[$i].bookTitle").value(post.bookTitle))
                .andExpect(
                    jsonPath("$.data.content[$i].createdDate").value(
                        Matchers.startsWith(
                            post.createdDate.toString().take(20)
                        )
                    )
                )
                .andExpect(
                    jsonPath("$.data.content[$i].modifiedDate").value(
                        Matchers.startsWith(
                            post.modifiedDate.toString().take(20)
                        )
                    )
                )
        }
    }
}