package com.bookbook.domain.rentBookList.service

import com.bookbook.domain.rentBookList.service.RentBookListService
import com.bookbook.domain.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.assertj.core.api.Assertions.assertThat

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("RentBookList Service 테스트")
class RentBookListServiceTest {

    @Autowired
    private lateinit var rentBookListService: RentBookListService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    @DisplayName("1. 대여 가능한 책 목록 조회 (기본)")
    fun test1() {
        val bookPage = rentBookListService.getAvailableBooks(
            page = 0, 
            size = 12, 
            region = null, 
            category = null, 
            search = null
        )

        // RentInitData에서 13개 테스트 데이터 생성됨
        assertThat(bookPage.content).hasSize(12) // 페이지 크기 만큼
        assertThat(bookPage.totalElements).isEqualTo(13) // 전체 13개
        assertThat(bookPage.totalPages).isEqualTo(2) // 13개를 12개씩 나누면 2페이지
    }

    @Test
    @DisplayName("2. 대여 가능한 책 목록 조회 (페이징)")
    fun test2() {
        val bookPage = rentBookListService.getAvailableBooks(
            page = 1, // 두 번째 페이지
            size = 5,
            region = null,
            category = null,
            search = null
        )

        // 13개 데이터를 5개씩 나누면: 1페이지 5개, 2페이지 5개, 3페이지 3개
        assertThat(bookPage.content).hasSize(5)
        assertThat(bookPage.totalElements).isEqualTo(13)
        assertThat(bookPage.number).isEqualTo(1) // 현재 페이지 번호 (0-based)
    }

    @Test
    @DisplayName("3. 대여 가능한 책 목록 조회 (지역 필터)")
    fun test3() {
        val bookPage = rentBookListService.getAvailableBooks(
            page = 0,
            size = 20,
            region = "서울특별시",
            category = null,
            search = null
        )

        // 서울특별시 지역의 책이 조회되는지 확인
        assertThat(bookPage.content).isNotEmpty()
        // RentInitData에서 서울특별시는 2권 ("더 좋은 삶을 위한 철학", "군주론")
        assertThat(bookPage.totalElements).isEqualTo(2)
    }

    @Test
    @DisplayName("4. 대여 가능한 책 목록 조회 (카테고리 필터)")
    fun test4() {
        val bookPage = rentBookListService.getAvailableBooks(
            page = 0,
            size = 20,
            region = null,
            category = "인문 에세이",
            search = null
        )

        // 인문 에세이 카테고리의 책이 조회되는지 확인
        assertThat(bookPage.content).isNotEmpty()
        // RentInitData에서 "인문 에세이"는 2권 ("더 좋은 삶을 위한 철학", "슬픔을 공부하는 슬픔")
        assertThat(bookPage.totalElements).isEqualTo(2)
    }

    @Test
    @DisplayName("5. 대여 가능한 책 목록 조회 (검색)")
    fun test5() {
        val bookPage = rentBookListService.getAvailableBooks(
            page = 0,
            size = 20,
            region = null,
            category = null,
            search = "한강"
        )

        // "한강" 검색 시 "소년이 온다" 1권이 조회되어야 함
        assertThat(bookPage.content).hasSize(1)
        assertThat(bookPage.totalElements).isEqualTo(1)
        assertThat(bookPage.content[0].bookTitle).contains("소년이 온다")
    }

    @Test
    @DisplayName("6. 대여 가능한 책 목록 조회 (복합 필터)")
    fun test6() {
        val bookPage = rentBookListService.getAvailableBooks(
            page = 0,
            size = 10,
            region = "서울특별시",
            category = "인문 에세이",
            search = null
        )

        // 서울특별시 + 인문 에세이 = "더 좋은 삶을 위한 철학" 1권
        assertThat(bookPage.totalElements).isEqualTo(1)
        assertThat(bookPage.content[0].bookTitle).contains("더 좋은 삶을 위한 철학")
    }

    @Test
    @DisplayName("7. 지역 목록 조회 테스트")
    fun test7() {
        val regions = rentBookListService.getRegions()

        // RentInitData에서 다양한 지역이 등록됨
        assertThat(regions).isNotEmpty()
        assertThat(regions).hasSizeGreaterThanOrEqualTo(10) // 최소 10개 지역
        
        // 지역 데이터 구조 확인
        val region = regions[0]
        assertThat(region).containsKeys("id", "name")
        assertThat(region["id"]).isNotNull()
        assertThat(region["name"]).isNotNull()
    }

    @Test
    @DisplayName("8. 카테고리 목록 조회 테스트")
    fun test8() {
        val categories = rentBookListService.getCategories()

        // RentInitData에서 다양한 카테고리가 등록됨
        assertThat(categories).isNotEmpty()
        assertThat(categories).hasSizeGreaterThanOrEqualTo(8) // 최소 8개 카테고리
        
        // 카테고리 데이터 구조 확인
        val category = categories[0]
        assertThat(category).containsKeys("id", "name")
        assertThat(category["id"]).isNotNull()
        assertThat(category["name"]).isNotNull()
    }

    @Test
    @DisplayName("9. 존재하지 않는 지역 필터 테스트")
    fun test9() {
        val bookPage = rentBookListService.getAvailableBooks(
            page = 0,
            size = 20,
            region = "존재하지않는지역",
            category = null,
            search = null
        )

        // 존재하지 않는 지역으로 필터링 시 빈 결과
        assertThat(bookPage.content).isEmpty()
        assertThat(bookPage.totalElements).isEqualTo(0)
    }

    @Test
    @DisplayName("10. 빈 검색어 처리 테스트")
    fun test10() {
        val bookPage = rentBookListService.getAvailableBooks(
            page = 0,
            size = 20,
            region = null,
            category = null,
            search = "   " // 공백만 있는 검색어
        )

        // 공백 검색어는 필터로 인식되지 않아 전체 데이터 반환
        assertThat(bookPage.totalElements).isEqualTo(13)
    }
}
