package com.bookbook.domain.home.service

import com.bookbook.domain.home.dto.BookInfoDto
import com.bookbook.domain.home.dto.HomeResponseDto
import com.bookbook.domain.home.dto.RegionInfoDto
import com.bookbook.domain.home.entity.Book
import com.bookbook.domain.home.repository.BookRepository
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 메인페이지 서비스
 */
@Service
@Transactional(readOnly = true)
class HomeService(
    private val bookRepository: BookRepository
) {
    
    companion object {
        private val log = LoggerFactory.getLogger(HomeService::class.java)
    }

    /**
     * 메인페이지 데이터 조회 - 지역별 최신 5개 도서 이미지
     * @param region 지역명 (선택적, 예: "관악구", "종로구")
     * @param request HTTP 요청 (사용자 지역 감지용)
     */
    fun getHomeData(region: String?, request: HttpServletRequest?): HomeResponseDto {
        val userRegion = detectUserRegion(request)

        val books: List<Book>
        val totalCount: Long
        val actualRegion: String

        if (!region.isNullOrBlank() && region != "전체") {
            // 특정 지역의 도서 조회 - 지역명을 단순화하여 검색
            val searchRegion = simplifyRegionName(region)
            log.debug("지역별 도서 조회: {} -> {}", region, searchRegion)
            books = bookRepository.findTop5WithImageByRegionOrderByIdDesc(searchRegion)
            totalCount = bookRepository.countBooksWithImageByRegion(searchRegion)
            actualRegion = region
        } else {
            // 전체 지역의 도서 조회
            books = bookRepository.findTop5WithImageOrderByIdDesc()
            totalCount = bookRepository.countBooksWithImage()
            actualRegion = "전체"
        }

        // 이미지 URL만 추출 (null과 빈 문자열 제외)
        val bookImages = books
            .mapNotNull { it.image }
            .filter { it.isNotBlank() }
        
        log.debug("조회된 도서 수: {}, 유효한 이미지 수: {}", books.size, bookImages.size)
        bookImages.forEachIndexed { index, imageUrl ->
            log.debug("이미지 {}: '{}'", index + 1, imageUrl)
        }

        return HomeResponseDto(
            region = actualRegion,
            bookImages = bookImages,
            totalBooksInRegion = totalCount,
            userRegion = userRegion
        )
    }

    /**
     * ID가 포함된 도서 정보 조회 (클릭 가능한 도서들)
     * @param region 지역명 (선택적)
     */
    fun getBooksWithId(region: String?): List<BookInfoDto> {
        val books: List<Book> = if (!region.isNullOrBlank() && region != "전체") {
            // 특정 지역의 도서 조회 - 지역명을 단순화하여 검색
            val searchRegion = simplifyRegionName(region)
            bookRepository.findTop5WithImageByRegionOrderByIdDesc(searchRegion)
        } else {
            // 전체 지역의 도서 조회
            bookRepository.findTop5WithImageOrderByIdDesc()
        }

        return books
            .filter { !it.image.isNullOrBlank() } // 이미지가 있는 도서만 필터링
            .map { book ->
                BookInfoDto(
                    id = book.id,
                    imageUrl = book.image,
                    title = book.title ?: "도서 ${book.id}",
                    bookTitle = book.title ?: "도서 ${book.id}"
                )
            }
    }

    /**
     * 지역 목록 조회
     */
    fun getRegions(): List<RegionInfoDto> {
        // 한국의 광역자치단체 목록 (기본값)
        val defaultRegions = listOf(
            RegionInfoDto(name = "서울특별시", code = "seoul"),
            RegionInfoDto(name = "부산광역시", code = "busan"),
            RegionInfoDto(name = "대구광역시", code = "daegu"),
            RegionInfoDto(name = "인천광역시", code = "incheon"),
            RegionInfoDto(name = "광주광역시", code = "gwangju"),
            RegionInfoDto(name = "대전광역시", code = "daejeon"),
            RegionInfoDto(name = "울산광역시", code = "ulsan"),
            RegionInfoDto(name = "경기도", code = "gyeonggi"),
            RegionInfoDto(name = "강원특별자치도", code = "gangwon"),
            RegionInfoDto(name = "충청북도", code = "chungbuk"),
            RegionInfoDto(name = "충청남도", code = "chungnam"),
            RegionInfoDto(name = "전북특별자치도", code = "jeonbuk"),
            RegionInfoDto(name = "전라남도", code = "jeonnam"),
            RegionInfoDto(name = "경상북도", code = "gyeongbuk"),
            RegionInfoDto(name = "경상남도", code = "gyeongnam"),
            RegionInfoDto(name = "제주특별자치도", code = "jeju")
        )

        return try {
            // DB에서 실제 등록된 지역들을 조회
            val dbRegions = bookRepository.findDistinctRegions()

            if (dbRegions.isNotEmpty()) {
                // DB에 등록된 지역들을 기본 지역 목록과 매칭하여 반환
                defaultRegions.filter { defaultRegion ->
                    dbRegions.contains(defaultRegion.name)
                }
            } else {
                defaultRegions
            }
        } catch (e: Exception) {
            log.warn("DB에서 지역 목록 조회 실패, 기본값 사용: ${e.message}")
            defaultRegions
        }
    }

    /**
     * 사용자 지역 감지 (회원 주소 기반)
     * 로그인한 사용자의 주소 정보에서 광역자치단체명을 추출하여 반환
     */
    private fun detectUserRegion(request: HttpServletRequest?): String? {
        return try {
            // Spring Security를 통해 현재 로그인한 사용자 정보 조회
            val userAddress = getCurrentUserAddress()
            if (!userAddress.isNullOrBlank()) {
                extractRegionFromAddress(userAddress)
            } else {
                null
            }
        } catch (e: Exception) {
            log.debug("사용자 지역 감지 실패: ${e.message}")
            null
        }
    }

    /**
     * 현재 로그인한 사용자의 주소 정보 조회
     */
    private fun getCurrentUserAddress(): String? {
        return try {
            // Spring Security Context에서 사용자 정보 가져오기
            val authentication = SecurityContextHolder.getContext().authentication

            if (authentication?.isAuthenticated == true && authentication.principal != "anonymousUser") {
                // CustomOAuth2User에서 주소 정보 추출
                if (authentication.principal is OAuth2User) {
                    val oauth2User = authentication.principal as OAuth2User
                    oauth2User.getAttribute<String>("address")
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            log.debug("사용자 주소 조회 실패: ${e.message}")
            null
        }
    }

    /**
     * 주소에서 광역자치단체명 추출
     * 예: "서울특별시 종로구 청운동" -> "서울특별시"
     */
    private fun extractRegionFromAddress(address: String?): String? {
        if (address.isNullOrBlank()) {
            return null
        }

        // 광역자치단체명 목록
        val regions = listOf(
            "서울특별시", "부산광역시", "대구광역시", "인천광역시",
            "광주광역시", "대전광역시", "울산광역시", "세종특별자치시",
            "경기도", "강원특별자치도", "충청북도", "충청남도",
            "전북특별자치도", "전라남도", "경상북도", "경상남도", "제주특별자치도"
        )

        // 주소에서 광역자치단체명 찾기
        for (region in regions) {
            if (address.contains(region)) {
                return region
            }
        }

        // 구 버전 명칭도 확인 (하위 호환성)
        return when {
            address.contains("강원도") -> "강원특별자치도"
            address.contains("전라북도") -> "전북특별자치도"
            else -> null
        }
    }

    /**
     * 지역명을 검색하기 쉽게 단순화
     * 예: "서울특별시" -> "서울", "경기도" -> "경기"
     */
    private fun simplifyRegionName(region: String?): String {
        if (region.isNullOrBlank()) {
            return ""
        }

        // 특별시, 광역시, 도 등을 제거하여 핵심 지역명만 추출
        return region
            .replace("특별시", "")
            .replace("광역시", "")
            .replace("특별자치도", "")
            .replace("자치도", "")
            .replace("도", "")
            .trim()
    }
}
