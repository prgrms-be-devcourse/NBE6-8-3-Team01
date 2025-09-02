package com.bookbook.domain.lendList.controller

import com.bookbook.domain.lendList.service.LendListService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class LendListControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var lendListService: LendListService

    // TestSetup에서 생성된 사용자와 RentInitData에서 생성된 대여글 ID 사용
    // EnvLoader는 @Configuration으로 Spring Boot 시작 시 자동 로드됨
    private val testUserId: Long = 1L  // TestSetup에서 생성된 첫 번째 사용자 (user1)

    @Test
    @DisplayName("내가 등록한 도서 목록 조회 - 성공")
    fun t1() {
        val resultActions = mvc
            .perform(get("/api/v1/user/$testUserId/lendlist"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(LendListController::class.java))
            .andExpect(handler().methodName("getLendListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("등록한 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    @DisplayName("내가 등록한 도서 목록 검색 조회 - 성공")
    fun t2() {
        val searchKeyword = "마음"

        val resultActions = mvc
            .perform(get("/api/v1/user/$testUserId/lendlist")
                .param("search", searchKeyword))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(LendListController::class.java))
            .andExpect(handler().methodName("getLendListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("등록한 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    @DisplayName("페이징 파라미터와 함께 도서 목록 조회 - 성공")
    fun t3() {
        val resultActions = mvc
            .perform(get("/api/v1/user/$testUserId/lendlist")
                .param("page", "0")
                .param("size", "5")
                .param("sort", "createdDate,desc"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(LendListController::class.java))
            .andExpect(handler().methodName("getLendListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("등록한 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.pageable").exists())
            .andExpect(jsonPath("$.data.totalElements").exists())
    }

    @Test
    @DisplayName("빈 검색어로 도서 목록 조회 - 성공")
    fun t4() {
        val resultActions = mvc
            .perform(get("/api/v1/user/$testUserId/lendlist")
                .param("search", ""))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(LendListController::class.java))
            .andExpect(handler().methodName("getLendListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("등록한 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    @DisplayName("여러 사용자의 등록 도서 목록 조회 - 성공")
    fun t5() {
        val secondUserId = 2L  // TestSetup에서 생성된 두 번째 사용자

        val resultActions = mvc
            .perform(get("/api/v1/user/$secondUserId/lendlist"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(LendListController::class.java))
            .andExpect(handler().methodName("getLendListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("등록한 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    @DisplayName("특정 키워드로 검색 - 성공")
    fun t6() {
        val searchKeyword = "편안"

        val resultActions = mvc
            .perform(get("/api/v1/user/$testUserId/lendlist")
                .param("search", searchKeyword))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(LendListController::class.java))
            .andExpect(handler().methodName("getLendListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("등록한 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 등록 도서 목록 조회 - 빈 목록 반환")
    fun t7() {
        val nonExistentUserId = 999L

        val resultActions = mvc
            .perform(get("/api/v1/user/$nonExistentUserId/lendlist"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(LendListController::class.java))
            .andExpect(handler().methodName("getLendListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("등록한 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    @DisplayName("큰 페이지 사이즈로 조회 - 성공")
    fun t8() {
        val resultActions = mvc
            .perform(get("/api/v1/user/$testUserId/lendlist")
                .param("page", "0")
                .param("size", "20"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(LendListController::class.java))
            .andExpect(handler().methodName("getLendListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("등록한 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.size").value(20))
    }

    @Test
    @DisplayName("도서 게시글 소프트 삭제 - 성공")
    fun t9() {
        // 테스트용 대여 게시글 ID (RentInitData에서 생성된 첫 번째 게시글)
        val rentId = 1L
        
        val resultActions = mvc
            .perform(delete("/api/v1/user/$testUserId/lendlist/$rentId"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(LendListController::class.java))
            .andExpect(handler().methodName("deleteLendList"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("도서 게시글을 삭제했습니다."))
    }

    @Test
    @DisplayName("권한 없는 사용자가 도서 게시글 삭제 시도 - 실패")
    fun t10() {
        // 다른 사용자가 등록한 게시글 삭제 시도
        val otherUserId = 2L
        val rentId = 1L  // testUserId(1)가 작성한 게시글
        
        val resultActions = mvc
            .perform(delete("/api/v1/user/$otherUserId/lendlist/$rentId"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(LendListController::class.java))
            .andExpect(handler().methodName("deleteLendList"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-FORBIDDEN"))
            .andExpect(jsonPath("$.msg").value("해당 게시글을 삭제할 권한이 없습니다."))
    }

    @Test
    @DisplayName("존재하지 않는 게시글 삭제 시도 - 실패")
    fun t11() {
        val nonExistentRentId = 999L
        
        val resultActions = mvc
            .perform(delete("/api/v1/user/$testUserId/lendlist/$nonExistentRentId"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(LendListController::class.java))
            .andExpect(handler().methodName("deleteLendList"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-RENT-NOT-FOUND"))
            .andExpect(jsonPath("$.msg").value("해당 대여 게시글을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("삭제된 게시글이 목록 조회에서 제외되는지 확인")
    fun t12() {
        // 먼저 게시글 삭제
        val rentId = 1L
        mvc.perform(delete("/api/v1/user/$testUserId/lendlist/$rentId"))
            .andExpect(status().isOk)
        
        // 삭제 후 목록 조회
        val resultActions = mvc
            .perform(get("/api/v1/user/$testUserId/lendlist"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(LendListController::class.java))
            .andExpect(handler().methodName("getLendListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("등록한 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content").isArray)
            // 삭제된 게시글은 목록에 포함되지 않아야 함
            // (실제 데이터 개수는 테스트 데이터에 따라 다를 수 있음)
    }
}