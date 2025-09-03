package com.bookbook.domain.rentList.controller

import com.bookbook.TestSetup
import com.bookbook.domain.rent.entity.RentStatus
import com.bookbook.domain.rent.repository.RentRepository
import com.bookbook.domain.rentList.entity.RentList
import com.bookbook.domain.rentList.entity.RentRequestStatus
import com.bookbook.domain.rentList.repository.RentListRepository
import com.bookbook.domain.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class RentListControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var rentRepository: RentRepository

    @Autowired
    private lateinit var rentListRepository: RentListRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var testSetup: TestSetup

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

    // ========== 도서 반납 관련 테스트 ==========

    @Test
    @DisplayName("반납 - 미인증 사용자는 401")
    fun t7() {
        val borrowerUserId = 1L
        val rentId = 1L

        SecurityContextHolder.clearContext()

        val resultActions = mvc
            .perform(patch("/api/v1/user/$borrowerUserId/rentlist/$rentId/return"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(RentListController::class.java))
            .andExpect(handler().methodName("returnBook"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.resultCode").value("401-1"))
    }

    @Test
    @DisplayName("반납 - 본인이 아닌 경우 403")
    fun t8() {
        val borrowerUserId = 1L
        val rentId = 1L

        val user2 = userRepository.findById(2L).orElseThrow()
        val principal = testSetup.createCustomOAuth2User(user2)
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        SecurityContextHolder.getContext().authentication = auth

        val resultActions = mvc
            .perform(patch("/api/v1/user/$borrowerUserId/rentlist/$rentId/return"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(RentListController::class.java))
            .andExpect(handler().methodName("returnBook"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-1"))
    }

    @Test
    @DisplayName("반납 - 진행 중인 대여 기록 없음 → 404")
    fun t9() {
        val borrowerUserId = 1L
        val rentId = 1L

        val user1 = userRepository.findById(borrowerUserId).orElseThrow()
        val principal = testSetup.createCustomOAuth2User(user1)
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        SecurityContextHolder.getContext().authentication = auth

        val resultActions = mvc
            .perform(patch("/api/v1/user/$borrowerUserId/rentlist/$rentId/return"))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(RentListController::class.java))
            .andExpect(handler().methodName("returnBook"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
    }

    @Test
    @DisplayName("반납 - 성공 시 200 및 상태 업데이트")
    fun t10() {
        val borrowerUserId = 1L
        val rentId = 1L

        val rent = rentRepository.findById(rentId).orElseThrow()
        rent.rentStatus = RentStatus.LOANED
        rentRepository.save(rent)

        val borrower = userRepository.findById(borrowerUserId).orElseThrow()
        val rentList = RentList(
            loanDate = LocalDateTime.now().minusDays(1),
            returnDate = LocalDateTime.now().plusDays(13),
            status = RentRequestStatus.APPROVED
        ).apply {
            this.borrowerUser = borrower
            this.rent = rent
        }
        rentListRepository.save(rentList)

        val principal = testSetup.createCustomOAuth2User(borrower)
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        SecurityContextHolder.getContext().authentication = auth

        val resultActions = mvc
            .perform(patch("/api/v1/user/$borrowerUserId/rentlist/$rentId/return")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(RentListController::class.java))
            .andExpect(handler().methodName("returnBook"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200"))

        val updatedRent = rentRepository.findById(rentId).orElseThrow()
        assert(updatedRent.rentStatus == RentStatus.FINISHED)

        val rentLists = rentListRepository.findByRentIdAndBorrowerUserId(rentId, borrowerUserId)
        assert(rentLists.any { it.id == rentList.id && it.status == RentRequestStatus.FINISHED })
    }
}
