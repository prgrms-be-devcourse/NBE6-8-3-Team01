package com.bookbook.domain.review.controller

import com.bookbook.domain.review.service.ReviewService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ReviewControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var reviewService: ReviewService

    // TestSetup에서 생성된 사용자와 RentInitData에서 생성된 대여글 ID 사용
    // EnvLoader는 @Configuration으로 Spring Boot 시작 시 자동 로드됨
    private val testLenderId: Long = 1L  // TestSetup에서 생성된 첫 번째 사용자 (user1)
    private val testBorrowerId: Long = 2L  // TestSetup에서 생성된 두 번째 사용자 (user2)
    private val testRentId: Long = 1L  // RentInitData에서 생성된 첫 번째 대여글

    @Test
    @DisplayName("대여자가 대여받은 사람에게 리뷰 작성 - 엔드포인트 구조 검증")
    fun t1() {
        val resultActions = mvc
            .perform(post("/api/v1/review/lender/$testLenderId/rent/$testRentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "rating": 5
                    }
                """))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(ReviewController::class.java))
            .andExpect(handler().methodName("createLenderReview"))
    }

    @Test
    @DisplayName("대여받은 사람이 대여자에게 리뷰 작성 - 엔드포인트 구조 검증")
    fun t2() {
        val resultActions = mvc
            .perform(post("/api/v1/review/borrower/$testBorrowerId/rent/$testRentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "rating": 4
                    }
                """))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(ReviewController::class.java))
            .andExpect(handler().methodName("createBorrowerReview"))
    }

    @Test
    @DisplayName("대여자 리뷰 작성 - 잘못된 경로 파라미터")
    fun t3() {
        val invalidLenderId = "invalid"

        val resultActions = mvc
            .perform(post("/api/v1/review/lender/$invalidLenderId/rent/$testRentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "rating": 5
                    }
                """))
            .andDo(print())

        resultActions
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("대여받은 사람 리뷰 작성 - 잘못된 경로 파라미터")
    fun t4() {
        val invalidBorrowerId = "invalid"

        val resultActions = mvc
            .perform(post("/api/v1/review/borrower/$invalidBorrowerId/rent/$testRentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "rating": 4
                    }
                """))
            .andDo(print())

        resultActions
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("대여자 리뷰 작성 - 잘못된 JSON 형식")
    fun t5() {
        val resultActions = mvc
            .perform(post("/api/v1/review/lender/$testLenderId/rent/$testRentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "invalid": "data"
                    }
                """))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(ReviewController::class.java))
            .andExpect(handler().methodName("createLenderReview"))
    }

    @Test
    @DisplayName("대여받은 사람 리뷰 작성 - 잘못된 JSON 형식")
    fun t6() {
        val resultActions = mvc
            .perform(post("/api/v1/review/borrower/$testBorrowerId/rent/$testRentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "invalid": "data"
                    }
                """))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(ReviewController::class.java))
            .andExpect(handler().methodName("createBorrowerReview"))
    }
}