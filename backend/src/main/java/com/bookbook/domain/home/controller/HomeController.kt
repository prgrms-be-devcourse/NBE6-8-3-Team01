package com.bookbook.domain.home.controller

import com.bookbook.domain.home.dto.BookInfoDto
import com.bookbook.domain.home.dto.HomeResponseDto
import com.bookbook.domain.home.dto.RegionInfoDto
import com.bookbook.domain.home.service.HomeService
import com.bookbook.global.rsdata.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/bookbook/home")
@Tag(name = "HomeController", description = "메인페이지 API 컨트롤러")
class HomeController(
    private val homeService: HomeService
) {
    
    companion object {
        private val log = LoggerFactory.getLogger(HomeController::class.java)
        
        // 응답 코드 상수
        private const val SUCCESS_HOME_FETCH = "200-1"
        private const val SUCCESS_HOME_MSG = "메인페이지 데이터를 성공적으로 불러왔습니다."
        private const val SUCCESS_BOOKS_FETCH = "200-2"
        private const val SUCCESS_BOOKS_MSG = "도서 정보를 성공적으로 불러왔습니다."
        private const val SUCCESS_REGIONS_FETCH = "200-3"
        private const val SUCCESS_REGIONS_MSG = "지역 목록을 성공적으로 불러왔습니다."
    }

    @GetMapping
    @Operation(
        summary = "메인페이지 데이터 조회",
        description = "이미지가 포함된 최신 5개 도서를 조회합니다. 지역 파라미터로 특정 지역의 도서만 조회 가능합니다.",
        tags = ["Home", "MainPage"]
    )
    fun getHomeData(
        @RequestParam(value = "region", required = false) region: String?,
        request: HttpServletRequest?
    ): RsData<HomeResponseDto> {
        log.debug("GET /api/v1/bookbook/home 요청 수신 - 지역: {}", region)

        val homeData = homeService.getHomeData(region, request)

        return RsData(
            SUCCESS_HOME_FETCH,
            homeData.message ?: SUCCESS_HOME_MSG,
            homeData
        )
    }

    @GetMapping("/books-with-id")
    @Operation(
        summary = "도서 정보 조회 (ID 포함)",
        description = "클릭 가능한 도서들의 ID와 상세 정보를 포함하여 조회합니다. 지역 파라미터로 특정 지역의 도서만 조회 가능합니다.",
        tags = ["Home", "Books"]
    )
    fun getBooksWithId(
        @RequestParam(value = "region", required = false) region: String?
    ): RsData<List<BookInfoDto>> {
        log.debug("GET /api/v1/bookbook/home/books-with-id 요청 수신 - 지역: {}", region)

        val books = homeService.getBooksWithId(region)

        return RsData(
            SUCCESS_BOOKS_FETCH,
            SUCCESS_BOOKS_MSG,
            books
        )
    }

    @GetMapping("/regions")
    @Operation(
        summary = "지역 목록 조회",
        description = "도서 등록이 가능한 지역 목록을 조회합니다.",
        tags = ["Home", "Regions"]
    )
    fun getRegions(): RsData<List<RegionInfoDto>> {
        log.debug("GET /api/v1/bookbook/home/regions 요청 수신")

        val regions = homeService.getRegions()

        return RsData(
            SUCCESS_REGIONS_FETCH,
            SUCCESS_REGIONS_MSG,
            regions
        )
    }
}
