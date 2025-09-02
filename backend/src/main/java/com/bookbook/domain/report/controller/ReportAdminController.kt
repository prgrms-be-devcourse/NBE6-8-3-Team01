package com.bookbook.domain.report.controller

import com.bookbook.domain.report.dto.response.ReportDetailResponseDto
import com.bookbook.domain.report.dto.response.ReportSimpleResponseDto
import com.bookbook.domain.report.enums.ReportStatus
import com.bookbook.domain.report.service.ReportService
import com.bookbook.global.extension.requireAdmin
import com.bookbook.global.jpa.dto.response.PageResponseDto
import com.bookbook.global.rsdata.RsData
import com.bookbook.global.security.CustomOAuth2User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/reports")
@Tag(name = "ReportAdminController", description = "어드민 전용 신고 관리 컨트롤러")
class ReportAdminController (
    private val reportService: ReportService
){
    private val log = LoggerFactory.getLogger(ReportAdminController::class.java)
    /**
     * 신고 글 목록을 가져옵니다.
     *
     *
     * 페이지 번호와 사이즈의 조합, 그리고 신고 처리 상태와
     * 신고 대상자 ID를 기반으로 필터링된 페이지를 가져올 수 있습니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 당 항목 수
     * @param status 신고처리 상태
     * @param targetUserId 신고 대상자 ID
     * @return 생성된 신고 글 페이지 정보
     */
    @GetMapping
    @Operation(summary = "신고 목록 조회")
    fun getReportPage(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) status: List<ReportStatus>?,
        @RequestParam(required = false) targetUserId: Long?,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): ResponseEntity<RsData<PageResponseDto<ReportSimpleResponseDto>>> {
        customOAuth2User.requireAdmin()

        val page = if (page < 1) 1 else page
        val size = if (size < 1) 10 else size

        val pageable: Pageable = PageRequest.of(page - 1, size)

        val reportHistoryPage = reportService.getReportPage(pageable, status, targetUserId)
        val response = PageResponseDto(reportHistoryPage)

        return ResponseEntity.ok(
            RsData(
                "200-1",
                "${response.pageInfo.totalElements}개의 신고글 조회 완료.",
                response
            )
        )
    }

    /**
     * 신고 글 하나의 상세 정보를 가져옵니다.
     *
     * @param reportId 신고 글 ID
     * @return 단일 신고 글 상세 정보
     */
    @GetMapping("/{reportId}/review")
    @Operation(summary = "단일 신고 상세 조회")
    fun getReportDetail(
        @PathVariable reportId: Long,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): ResponseEntity<RsData<ReportDetailResponseDto>> {
        customOAuth2User.requireAdmin()

        val reportDetail = reportService.getReportDetail(reportId)

        return ResponseEntity.ok(
            RsData(
                "200-1",
                "${reportId}번 신고글 조회 완료",
                reportDetail
            )
        )
    }

    /**
     * 신고 글 하나를 처리 완료 상태로 변경합니다.
     *
     * @param reportId 신고 글 ID
     */
    @PatchMapping("/{reportId}/process")
    @Operation(summary = "단일 신고 처리 완료")
    fun processReport(
        @PathVariable reportId: Long,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): ResponseEntity<RsData<Void>> {
        val customOAuth2User = customOAuth2User.requireAdmin()

        reportService.markReportAsProcessed(reportId, customOAuth2User.userId)
        log.info("{}번 신고 처리 완료", reportId)

        return ResponseEntity.ok(
            RsData("200-1", "${reportId}번 신고가 정상적으로 처리되었습니다.")
        )
    }
}
