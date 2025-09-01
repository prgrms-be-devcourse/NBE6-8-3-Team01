package com.bookbook.domain.home

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
@DisplayName("Home API 통합 테스트")
class HomeApiTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @DisplayName("1-1. 전체 지역 메인페이지 조회 테스트")
    fun test1_1() {
        mockMvc.perform(get("/api/v1/bookbook/home"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").exists())
            .andExpect(jsonPath("$.data.region").value("전체"))
            .andExpect(jsonPath("$.data.bookImages").isArray)
            .andExpect(jsonPath("$.data.totalBooksInRegion").isNumber)
    }

    @Test
    @DisplayName("1-2. 특정 지역 메인페이지 조회 테스트")
    fun test1_2() {
        mockMvc.perform(get("/api/v1/bookbook/home")
                .param("region", "서울특별시"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.region").value("서울특별시"))
            .andExpect(jsonPath("$.data.bookImages").isArray)
            .andExpect(jsonPath("$.data.totalBooksInRegion").isNumber)
    }

    @Test
    @DisplayName("1-3. 존재하지 않는 지역 처리 테스트")
    fun test1_3() {
        mockMvc.perform(get("/api/v1/bookbook/home")
                .param("region", "존재하지않는지역"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalBooksInRegion").value(0))
    }

    @Test
    @DisplayName("1-4. ID 포함 도서 정보 조회 (전체 지역) 테스트")
    fun test1_4() {
        mockMvc.perform(get("/api/v1/bookbook/home/books-with-id"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-2"))
            .andExpect(jsonPath("$.msg").value("도서 정보를 성공적으로 불러왔습니다."))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    @DisplayName("1-5. ID 포함 도서 정보 조회 (특정 지역) 테스트")
    fun test1_5() {
        mockMvc.perform(get("/api/v1/bookbook/home/books-with-id")
                .param("region", "경기도"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-2"))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    @DisplayName("2. 지역 목록 조회 API 테스트")
    fun test2() {
        mockMvc.perform(get("/api/v1/bookbook/home/regions"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-3"))
            .andExpect(jsonPath("$.msg").value("지역 목록을 성공적으로 불러왔습니다."))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").exists())
            .andExpect(jsonPath("$.data[0].name").exists())
            .andExpect(jsonPath("$.data[0].code").exists())
    }

    @Test
    @DisplayName("3. API 응답 구조 검증 테스트")
    fun test3() {
        // 3-1. 메인페이지 응답 구조 검증
        mockMvc.perform(get("/api/v1/bookbook/home"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").exists())
            .andExpect(jsonPath("$.msg").exists())
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.region").exists())
            .andExpect(jsonPath("$.data.bookImages").exists())
            .andExpect(jsonPath("$.data.totalBooksInRegion").exists())

        // 3-2. 도서 정보 응답 구조 검증
        mockMvc.perform(get("/api/v1/bookbook/home/books-with-id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-2"))
            .andExpect(jsonPath("$.data").isArray)

        // 3-3. 지역 목록 응답 구조 검증
        mockMvc.perform(get("/api/v1/bookbook/home/regions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-3"))
            .andExpect(jsonPath("$.data").isArray)
    }
}
