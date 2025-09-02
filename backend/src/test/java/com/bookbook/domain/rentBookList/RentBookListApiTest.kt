package com.bookbook.domain.rentBookList

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("RentBookList API 통합 테스트")
class RentBookListApiTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @DisplayName("1-1. 대여 가능한 책 목록 조회 (기본)")
    fun test1_1() {
        mockMvc.perform(get("/api/v1/bookbook/rent/available"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("대여 가능한 책 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.pageInfo").exists())
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(13)) // 13개 테스트 데이터
            .andExpect(jsonPath("$.data.pageInfo.currentPage").value(1))
    }

    @Test
    @DisplayName("1-2. 대여 가능한 책 목록 조회 (페이징)")
    fun test1_2() {
        mockMvc.perform(get("/api/v1/bookbook/rent/available")
                .param("page", "2")
                .param("size", "5"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageInfo.currentPage").value(2))
            .andExpect(jsonPath("$.data.pageInfo.size").value(5))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(13))
    }

    @Test
    @DisplayName("1-3. 대여 가능한 책 목록 조회 (지역 필터)")
    fun test1_3() {
        mockMvc.perform(get("/api/v1/bookbook/rent/available")
                .param("region", "서울특별시"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    @DisplayName("1-4. 대여 가능한 책 목록 조회 (카테고리 필터)")
    fun test1_4() {
        mockMvc.perform(get("/api/v1/bookbook/rent/available")
                .param("category", "소설"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    @DisplayName("1-5. 대여 가능한 책 목록 조회 (검색)")
    fun test1_5() {
        mockMvc.perform(get("/api/v1/bookbook/rent/available")
                .param("search", "한강"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1)) // "소년이 온다" 1권
    }

    @Test
    @DisplayName("1-6. 대여 가능한 책 목록 조회 (복합 필터)")
    fun test1_6() {
        mockMvc.perform(get("/api/v1/bookbook/rent/available")
                .param("region", "서울특별시")
                .param("category", "인문 에세이")
                .param("page", "1")
                .param("size", "10"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1)) // 서울+인문에세이 = "더 좋은 삶을 위한 철학" 1권
    }

    @Test
    @DisplayName("2. 지역 목록 조회 테스트")
    fun test2() {
        mockMvc.perform(get("/api/v1/bookbook/rent/regions"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("지역 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    @DisplayName("3. 카테고리 목록 조회 테스트")
    fun test3() {
        mockMvc.perform(get("/api/v1/bookbook/rent/categories"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("카테고리 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    @DisplayName("4. API 응답 구조 검증 테스트")
    fun test4() {
        // 4-1. 페이징 응답 구조 검증
        mockMvc.perform(get("/api/v1/bookbook/rent/available"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").exists())
            .andExpect(jsonPath("$.msg").exists())
            .andExpect(jsonPath("$.data.content").exists())
            .andExpect(jsonPath("$.data.pageInfo").exists())
            .andExpect(jsonPath("$.data.pageInfo.totalElements").exists())
            .andExpect(jsonPath("$.data.pageInfo.totalPages").exists())

        // 4-2. 지역 목록 응답 구조 검증
        mockMvc.perform(get("/api/v1/bookbook/rent/regions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)

        // 4-3. 카테고리 목록 응답 구조 검증
        mockMvc.perform(get("/api/v1/bookbook/rent/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)
    }
}
