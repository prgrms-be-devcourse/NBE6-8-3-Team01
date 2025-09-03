package com.bookbook.domain.rent.controller

import com.bookbook.domain.rent.dto.request.ChangeRentStatusRequestDto
import com.bookbook.domain.rent.dto.response.RentDetailResponseDto
import com.bookbook.domain.rent.dto.response.RentSimpleResponseDto
import com.bookbook.domain.rent.entity.RentStatus
import com.bookbook.domain.rent.service.RentService
import com.bookbook.global.exception.ServiceException
import com.bookbook.global.jpa.dto.response.PageResponseDto
import com.bookbook.global.rsdata.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// 25.08.29 현준
// Kotlin으로 변환된 RentAdminController - 어드민 전용 RESTful 웹 서비스 컨트롤러
@RestController
@RequestMapping("/api/v1/admin/rent")
@Tag(name = "RentAdminController", description = "어드민 전용 대여 게시글 컨트롤러")
class RentAdminController(
    private val rentService: RentService
) {

    /**
     * 대여 게시글 작성 목록을 가져옵니다.
     *
     * 페이지 번호와 사이즈의 조합, 그리고 책 대여 상태와
     * 글 작성자 ID를 기반으로 필터링된 페이지를 가져올 수 있습니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 당 항목 수
     * @param status 리뷰 생성 요청 데이터 (평점)
     * @param userId 대여 게시글 작성자 ID
     * @return 생성된 대여 게시글 페이지 정보
     */
    @GetMapping
    @Operation(summary = "대여 게시글 목록 조회")
    fun getPosts(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) status: List<RentStatus>?,
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<RsData<PageResponseDto<RentSimpleResponseDto>>> {
        val page = if (page < 1) 1 else page
        val size = if (size < 1) 10 else size

        val pageable: Pageable = PageRequest.of(page - 1, size)

        val rentHistoryPage = rentService.getRentsPage(pageable, status, userId)
        val response = PageResponseDto(rentHistoryPage)

        return ResponseEntity.ok(
            RsData(
                "200-1",
                "${rentHistoryPage.totalElements}개의 글을 발견했습니다.",
                response
            )
        )
    }

    /**
     * 대여 게시글 하나의 상세 정보를 가져옵니다.
     *
     * @param id 대여 게시글 ID
     * @return 단일 대여 게시글 상세 정보
     */
    @GetMapping("/{id}")
    @Operation(summary = "단일 대여 게시글 상세 조회")
    fun getRentDetail(
        @PathVariable id: Long
    ): ResponseEntity<RsData<RentDetailResponseDto>> {
        val responseDto = rentService.getRentPostDetail(id)

        return ResponseEntity.ok(
            RsData("200-1", "${id}번 글 조회 완료.", responseDto)
        )
    }

    /**
     * 대여 게시글 하나의 상태를 수정합니다.
     *
     * @param id 대여 게시글 ID
     * @return 단일 대여 게시글 수정 후 상세 정보
     */
    @PatchMapping("/{id}")
    @Operation(summary = "대여 게시글 상태 수정")
    fun changeRentStatus(
        @PathVariable id: Long,
        @RequestBody status: ChangeRentStatusRequestDto
    ): ResponseEntity<RsData<RentDetailResponseDto>> { // 경로 변수로 전달된 id를 사용
        val responseDto = rentService.modifyRentPageStatus(id, status.status)

        return ResponseEntity.ok(
            RsData("200-1", "${id}번 글 상태 변경 완료.", responseDto)
        )
    }

    /**
     * SOFT DELETE된 대여 게시글 하나를 복구합니다.
     *
     * @param id 대여 게시글 ID
     * @return 복구 완료된 게시글의 정보
     */
    @PatchMapping("/{id}/restore")
    @Operation(summary = "대여 게시글 복구")
    fun restoreRentPage(
      @PathVariable id: Long
    ): ResponseEntity<RsData<RentDetailResponseDto>> { // 경로 변수로 전달된 id를 사용
        val responseDto = rentService.restoreRentPage(id)

        return ResponseEntity.ok(
            RsData(
                "200-1", "${id}번 글 복구 완료.", responseDto
            )
        )
    }
}