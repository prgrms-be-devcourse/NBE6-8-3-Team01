package com.bookbook.domain.home.service

import com.bookbook.domain.home.service.HomeService
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
@DisplayName("Home Service 테스트")
class HomeServiceTest {

    @Autowired
    private lateinit var homeService: HomeService

    @Test
    @DisplayName("1. 메인페이지 데이터 조회 (전체 지역)")
    fun test1() {
        val homeData = homeService.getHomeData(null, null)

        // RentInitData에서 13개 테스트 데이터 확인
        assertThat(homeData.region).isEqualTo("전체")
        assertThat(homeData.bookImages).isNotEmpty()
        assertThat(homeData.totalBooksInRegion).isGreaterThanOrEqualTo(5) // 최소 5개 이미지
    }

    @Test
    @DisplayName("2. 메인페이지 데이터 조회 (특정 지역)")
    fun test2() {
        val homeData = homeService.getHomeData("서울특별시", null)

        assertThat(homeData.region).isEqualTo("서울특별시")
        assertThat(homeData.totalBooksInRegion).isGreaterThanOrEqualTo(0) // 해당 지역의 책 개수
    }

    @Test
    @DisplayName("3. 메인페이지 데이터 조회 (존재하지 않는 지역)")
    fun test3() {
        val homeData = homeService.getHomeData("존재하지않는지역", null)

        assertThat(homeData.region).isEqualTo("존재하지않는지역")
        assertThat(homeData.totalBooksInRegion).isEqualTo(0)
        assertThat(homeData.bookImages).isEmpty()
    }

    @Test
    @DisplayName("4. ID 포함 도서 정보 조회 (전체 지역)")
    fun test4() {
        val booksWithId = homeService.getBooksWithId(null)

        // 이미지가 있는 도서들만 반환
        assertThat(booksWithId).isNotEmpty()
        
        // 첫 번째 도서 정보 확인
        if (booksWithId.isNotEmpty()) {
            val firstBook = booksWithId[0]
            assertThat(firstBook.id).isNotNull()
            assertThat(firstBook.imageUrl).isNotNull()
            assertThat(firstBook.title).isNotNull()
        }
    }

    @Test
    @DisplayName("5. ID 포함 도서 정보 조회 (특정 지역)")
    fun test5() {
        val booksWithId = homeService.getBooksWithId("경기도")

        // 경기도 지역의 도서들 (없을 수도 있음)
        assertThat(booksWithId).isNotNull()
        
        // 도서가 있다면 ID와 이미지가 포함되어야 함
        booksWithId.forEach { book ->
            assertThat(book.id).isNotNull()
            assertThat(book.imageUrl).isNotNull()
        }
    }

    @Test
    @DisplayName("6. 지역 목록 조회 테스트")
    fun test6() {
        val regions = homeService.getRegions()

        // 기본 광역자치단체 목록이 조회되어야 함
        assertThat(regions).isNotEmpty()
        assertThat(regions).hasSizeGreaterThanOrEqualTo(10)
        
        // 지역 데이터 구조 확인
        val region = regions[0]
        assertThat(region.name).isNotNull()
        assertThat(region.code).isNotNull()
    }

    @Test
    @DisplayName("7. 빈 지역 파라미터 처리")
    fun test7() {
        // 빈 문자열과 null은 모두 "전체"로 처리되어야 함
        val homeDataEmpty = homeService.getHomeData("", null)
        val homeDataNull = homeService.getHomeData(null, null)

        assertThat(homeDataEmpty.region).isEqualTo("전체")
        assertThat(homeDataNull.region).isEqualTo("전체")
        assertThat(homeDataEmpty.totalBooksInRegion).isEqualTo(homeDataNull.totalBooksInRegion)
    }

    @Test
    @DisplayName("8. 전체 지역 명시 처리")
    fun test8() {
        // "전체" 지역을 명시적으로 요청하는 경우
        val homeData = homeService.getHomeData("전체", null)

        assertThat(homeData.region).isEqualTo("전체")
        assertThat(homeData.totalBooksInRegion).isGreaterThanOrEqualTo(0)
    }

    @Test
    @DisplayName("9. 도서 이미지 URL 형식 검증")
    fun test9() {
        val homeData = homeService.getHomeData(null, null)

        // 모든 이미지 URL이 null이 아니고 빈 문자열이 아닌지 확인
        homeData.bookImages.forEach { imageUrl ->
            assertThat(imageUrl).isNotNull()
            assertThat(imageUrl).isNotEmpty()
        }
    }

    @Test
    @DisplayName("10. 지역별 도서 필터링 검증")
    fun test10() {
        // 특정 지역의 도서 수가 전체보다 작거나 같은지 확인
        val totalBooks = homeService.getHomeData(null, null).totalBooksInRegion
        val seoulBooks = homeService.getHomeData("서울특별시", null).totalBooksInRegion

        assertThat(seoulBooks).isLessThanOrEqualTo(totalBooks)
    }

    @Test
    @DisplayName("11. ID 포함 도서와 메인페이지 데이터 일관성")
    fun test11() {
        // 전체 지역의 경우 getBooksWithId와 getHomeData의 결과가 일관성 있어야 함
        val homeData = homeService.getHomeData(null, null)
        val booksWithId = homeService.getBooksWithId(null)

        // 둘 다 이미지가 있는 도서만 반환하므로 개수가 같거나 비슷해야 함
        assertThat(booksWithId.size).isLessThanOrEqualTo(homeData.bookImages.size + 5) // 여유분 고려
    }

    @Test
    @DisplayName("12. 지역 목록에 서울특별시 포함 확인")
    fun test12() {
        val regions = homeService.getRegions()

        // 기본 지역 목록에 서울특별시가 포함되어 있는지 확인
        val hasSeoul = regions.any { it.name == "서울특별시" }
        assertThat(hasSeoul).isTrue()
    }
}
