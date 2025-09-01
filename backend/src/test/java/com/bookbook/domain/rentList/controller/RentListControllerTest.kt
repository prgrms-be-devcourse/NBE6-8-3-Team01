package com.bookbook.domain.rentList.controller

import com.bookbook.domain.rentList.service.RentListService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class RentListControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var rentListService: RentListService

    // TestSetup에서 생성된 사용자와 RentInitData에서 생성된 대여글 ID 사용
    // EnvLoader는 @Configuration으로 Spring Boot 시작 시 자동 로드됨
    private val testBorrowerUserId: Long = 1L  // TestSetup에서 생성된 첫 번째 사용자 (user1)
    private val testRentId: Long = 1L  // RentInitData에서 생성된 첫 번째 대여글

    @Test
    @DisplayName("내가 빌린 도서 목록 조회 - 성공")
    fun t1() {
        val resultActions = mvc
            .perform(get("/api/v1/user/$testBorrowerUserId/rentlist"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(RentListController::class.java))
            .andExpect(handler().methodName("getRentListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("대여 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
    }

    @Test
    @DisplayName("내가 빌린 도서 목록 검색 조회 - 성공")
    fun t2() {
        val searchKeyword = "테스트"

        val resultActions = mvc
            .perform(get("/api/v1/user/$testBorrowerUserId/rentlist")
                .param("search", searchKeyword))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(RentListController::class.java))
            .andExpect(handler().methodName("getRentListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("대여 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
    }

    @Test
    @DisplayName("빈 검색어로 도서 목록 조회 - 성공")
    fun t3() {
        val resultActions = mvc
            .perform(get("/api/v1/user/$testBorrowerUserId/rentlist")
                .param("search", ""))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(RentListController::class.java))
            .andExpect(handler().methodName("getRentListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("대여 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
    }

    @Test
    @DisplayName("여러 사용자의 대여 목록 조회 - 성공")
    fun t4() {
        val secondUserId = 2L  // TestSetup에서 생성된 두 번째 사용자

        val resultActions = mvc
            .perform(get("/api/v1/user/$secondUserId/rentlist"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(RentListController::class.java))
            .andExpect(handler().methodName("getRentListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("대여 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
    }

    @Test
    @DisplayName("특정 키워드로 검색 - 성공")
    fun t5() {
        val searchKeyword = "마음"

        val resultActions = mvc
            .perform(get("/api/v1/user/$testBorrowerUserId/rentlist")
                .param("search", searchKeyword))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(RentListController::class.java))
            .andExpect(handler().methodName("getRentListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("대여 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 대여 목록 조회 - 빈 목록 반환")
    fun t6() {
        val nonExistentUserId = 999L

        val resultActions = mvc
            .perform(get("/api/v1/user/$nonExistentUserId/rentlist"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(RentListController::class.java))
            .andExpect(handler().methodName("getRentListByUserId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("대여 도서 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").isArray)
    }
}