package com.bookbook.domain.wishList.controller

import com.bookbook.domain.wishList.service.WishListService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class WishListControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var wishListService: WishListService

    // TestSetup에서 생성된 사용자와 RentInitData에서 생성된 대여글 ID 사용
    // EnvLoader는 @Configuration으로 Spring Boot 시작 시 자동 로드됨
    private val testUserId: Long = 1L  // TestSetup에서 생성된 첫 번째 사용자 (user1)
    private val testRentId: Long = 1L  // RentInitData에서 생성된 첫 번째 대여글

    @Test
    @DisplayName("찜 목록 조회 - 성공")
    fun t1() {
        val resultActions = mvc
            .perform(get("/api/v1/user/$testUserId/wishlist"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(WishListController::class.java))
            .andExpect(handler().methodName("getWishList"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("찜 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
    }

    @Test
    @DisplayName("찜 목록 검색 조회 - 성공")
    fun t2() {
        val searchKeyword = "테스트"

        val resultActions = mvc
            .perform(get("/api/v1/user/$testUserId/wishlist")
                .param("search", searchKeyword))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(WishListController::class.java))
            .andExpect(handler().methodName("getWishList"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("찜 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").exists())
    }

    @Test
    @DisplayName("찜 목록에 도서 추가 - 성공")
    fun t3() {
        val resultActions = mvc
            .perform(post("/api/v1/user/$testUserId/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "rentId": $testRentId
                    }
                """))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(WishListController::class.java))
            .andExpect(handler().methodName("addWishList"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("찜 목록에 추가했습니다."))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.rentId").value(testRentId))
    }

    @Test
    @DisplayName("찜 목록에서 도서 삭제 - 성공")
    fun t4() {
        // 먼저 찜 목록에 추가
        mvc.perform(post("/api/v1/user/$testUserId/wishlist")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "rentId": $testRentId
                }
            """))

        // 삭제
        val resultActions = mvc
            .perform(delete("/api/v1/user/$testUserId/wishlist/$testRentId"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(WishListController::class.java))
            .andExpect(handler().methodName("deleteWishList"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.msg").value("찜 목록에서 삭제했습니다."))
    }

    @Test
    @DisplayName("찜 목록 추가 - 존재하지 않는 사용자")
    fun t5() {
        val userId = 999L // 존재하지 않는 userId

        val resultActions = mvc
            .perform(post("/api/v1/user/$userId/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "rentId": $testRentId
                    }
                """))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(WishListController::class.java))
            .andExpect(handler().methodName("addWishList"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404"))
            .andExpect(jsonPath("$.msg").value("사용자를 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("찜 목록 추가 - 존재하지 않는 대여 게시글")
    fun t6() {
        val rentId = 999L // 존재하지 않는 rentId

        val resultActions = mvc
            .perform(post("/api/v1/user/$testUserId/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "rentId": $rentId
                    }
                """))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(WishListController::class.java))
            .andExpect(handler().methodName("addWishList"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404"))
            .andExpect(jsonPath("$.msg").value("대여 게시글을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("찜 목록에서 도서 삭제 - 찜하지 않은 도서 삭제 실패")
    fun t7() {
        val rentId = 999L // 존재하지 않는 rentId

        val resultActions = mvc
            .perform(delete("/api/v1/user/$testUserId/wishlist/$rentId"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(WishListController::class.java))
            .andExpect(handler().methodName("deleteWishList"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404"))
            .andExpect(jsonPath("$.msg").value("찜하지 않은 게시글입니다."))
    }

    @Test
    @DisplayName("찜 목록에 도서 추가 - 중복 추가 실패")
    fun t8() {
        // 첫 번째 추가
        mvc.perform(post("/api/v1/user/$testUserId/wishlist")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "rentId": $testRentId
                }
            """))

        // 중복 추가 시도
        val resultActions = mvc
            .perform(post("/api/v1/user/$testUserId/wishlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "rentId": $testRentId
                    }
                """))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(WishListController::class.java))
            .andExpect(handler().methodName("addWishList"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.resultCode").value("409"))
            .andExpect(jsonPath("$.msg").value("이미 찜한 게시글입니다."))
    }
}