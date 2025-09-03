package com.bookbook.domain.report.service

import com.bookbook.domain.report.dto.response.ReportDetailResponseDto
import com.bookbook.domain.report.dto.response.ReportSimpleResponseDto
import com.bookbook.domain.report.entity.Report
import com.bookbook.domain.report.enums.ReportStatus
import com.bookbook.domain.report.repository.ReportRepository
import com.bookbook.domain.user.service.UserService
import com.bookbook.global.exception.ServiceException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val userService: UserService
) {

    /**
     * 신고글을 생성합니다.
     *
     * @param reporterUserId 페이지 기본 정보
     * @param targetUserId 대여 글 상태의 리스트
     * @param reason 신고 대상자 ID
     * @throws ServiceException
     * <p>(400) 신고자 본인을 신고하려고 할 때
     * <p>(404) 신고와 관련한 유저를 찾지 못했을 때
     */
    @Transactional
    fun createReport(reporterUserId: Long, targetUserId: Long, reason: String) {
        if (reporterUserId == targetUserId)
            throw ServiceException("400-REPORT-SELF", "자기 자신을 신고할 수 없습니다.")

        userService.findById(reporterUserId)
            ?: throw ServiceException("404-USER-NOT-FOUND", "신고한 사용자를 찾을 수 없습니다.")

        userService.findById(targetUserId)
            ?: throw ServiceException("404-USER-NOT-FOUND", "신고 대상 사용자를 찾을 수 없습니다.")

        val report = Report(
            reporterUserId,
            targetUserId,
            reason
        )

        reportRepository.save(report)
    }

    /**
     * 대여 글을 페이지로 가져옵니다.
     *
     * @param pageable 페이지 기본 정보
     * @param status 대여 글 상태의 리스트
     * @param targetUserId 신고 대상자 ID
     * @return 생성된 신고 글 페이지 정보
     */
    @Transactional(readOnly = true)
    fun getReportPage(
        pageable: Pageable,
        status: List<ReportStatus>?,
        targetUserId: Long?
    ): Page<ReportSimpleResponseDto> {
        val reportPage = reportRepository
            .findFilteredReportHistory(pageable, status, targetUserId)

        return reportPage.map { ReportSimpleResponseDto(it) }
    }

    /**
     * 신고 글 하나의 상세 정보를 가져옵니다.
     * <p>관리자가 최초로 상세 정보를 요청하면 자동으로 검토 중 상태로 변경됩니다
     *
     * @param reportId 신고 글 ID
     * @return 단일 신고 글 상세 정보
     * @throws ServiceException (404) 신고 글이 존재하지 않은 경우
     */
    @Transactional
    fun getReportDetail(reportId: Long): ReportDetailResponseDto {
        val report = findReportById(reportId)

        if (report.status == ReportStatus.PENDING) {
            report.markAsReviewed()
        }

        return ReportDetailResponseDto(report)
    }

    /**
     * 신고 글 하나를 처리 완료 상태로 변경합니다.
     *
     * @param reportId 신고 글 ID
     * @param userId 처리 완료를 진행한 자의 ID
     * @throws ServiceException
     * <p>(401) 허가되지 않은 접근
     * <p>(409) 처리가 완료된 신고를 다시 처리하고자 할 때
     * <p>(422) 대기 중인 신고를 처리 완료 상태로 바꾸고자 할 때
     */
    @Transactional
    fun markReportAsProcessed(reportId: Long, userId: Long) {
        val closer = userService.findById(userId)
            ?. takeIf { it.isAdmin }
            ?: throw ServiceException("401-1", "허가되지 않은 접근입니다 처리할 권한이 없습니다.")

        val report = findReportById(reportId)

        val status = report.status

        if (status == ReportStatus.PENDING)
            throw ServiceException("422-1", "해당 신고를 먼저 확인해야 합니다.")

        if (status == ReportStatus.PROCESSED)
            throw ServiceException("409-1", "해당 신고는 이미 처리가 완료되었습니다.")

        // 신고 이슈를 닫은 사람을 표기할 수 있도록
        report.markAsProcessed(closer)
    }

    /**
     * 신고 글 하나를 리포지토리에서 검색합니다.
     *
     * @param reportId 신고 글 ID
     * @throws ServiceException (404) 신고 글이 존재하지 않은 경우
     */
    @Transactional(readOnly = true)
    fun findReportById(reportId: Long): Report {
        return reportRepository.findById(reportId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 신고입니다.") }
    }
}
