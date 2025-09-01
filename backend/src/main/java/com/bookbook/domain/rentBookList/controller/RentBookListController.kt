package com.bookbook.domain.rentBookList.controller

import com.bookbook.domain.rentBookList.dto.RentBookListResponseDto
import com.bookbook.domain.rentBookList.dto.RentRequestDto
import com.bookbook.domain.rentBookList.service.RentBookListService
import com.bookbook.global.jpa.dto.response.PageResponseDto
import com.bookbook.global.rsdata.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/bookbook/rent")
@Tag(name = "RentBookListController", description = "책 빌리러가기 API 컨트롤러")
class RentBookListController(
    private val rentBookListService: RentBookListService
) {
    private val log = LoggerFactory.getLogger(RentBookListController::class.java)

    @GetMapping("/available")
    @Operation(summary = "대여 가능한 책 목록 조회", description = "필터링과 페이징을 지원하는 대여 가능한 책 목록을 조회합니다.")
    fun getAvailableBooks(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "12") size: Int,
        @RequestParam(required = false) region: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) search: String?
    ): RsData<PageResponseDto<RentBookListResponseDto>?> {
        log.debug(
            "대여 가능한 책 목록 조회 - page: {}, size: {}, region: {}, category: {}, search: {}",
            page, size, region, category, search
        )

        val bookPage = rentBookListService.getAvailableBooks(
            page - 1, size, region, category, search
        )

        val response = PageResponseDto(bookPage)

        return RsData("200-1", "대여 가능한 책 목록을 조회했습니다.", response)
    }

    @GetMapping("/{rentId}")
    @Operation(summary = "책 상세 정보 조회", description = "특정 책의 상세 정보를 조회합니다.")
    fun getBookDetail(
        @PathVariable rentId: Long
    ): RsData<RentBookListResponseDto?> {
        log.debug("책 상세 정보 조회 - rentId: {}", rentId)

        val bookDetail = rentBookListService.getBookDetail(rentId)

        return RsData("200-3", "책 상세 정보를 조회했습니다.", bookDetail)
    }

    @PostMapping("/{rentId}/request")
    @Operation(summary = "대여 신청", description = "특정 책에 대해 대여 신청을 합니다.")
    fun requestRent(
        @PathVariable rentId: Long,
        @RequestBody requestDto: RentRequestDto
    ): RsData<Void?> {
        log.debug("대여 신청 - rentId: {}, message: {}", rentId, requestDto.message)

        rentBookListService.requestRent(rentId, requestDto.message)

        return RsData("200-1", "대여 신청이 완료되었습니다.")
    }

    @GetMapping("/regions")
    @Operation(summary = "지역 목록 조회", description = "등록된 책들의 지역 목록을 조회합니다.")
    fun getRegions(): RsData<List<Map<String, String>>?> {
        log.debug("지역 목록 조회")

        val regions = rentBookListService.getRegions()

        return RsData("200-1", "지역 목록을 조회했습니다.", regions)
    }

    @GetMapping("/categories")
    @Operation(summary = "카테고리 목록 조회", description = "등록된 책들의 카테고리 목록을 조회합니다.")
    fun getCategories(): RsData<List<Map<String, String>>?> {
        log.debug("카테고리 목록 조회")

        val categories = rentBookListService.getCategories()

        return RsData("200-1", "카테고리 목록을 조회했습니다.", categories)
    }
}
