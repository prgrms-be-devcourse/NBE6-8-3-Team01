package com.bookbook.domain.report.dto.response

import com.bookbook.domain.report.entity.Report
import com.bookbook.domain.report.enums.ReportStatus
import java.time.LocalDateTime

data class ReportSimpleResponseDto(
    val id: Long,
    val status: ReportStatus,
    val reporterUserId: Long,
    val targetUserId: Long,
    val createdDate: LocalDateTime
) {
    companion object {
        @JvmStatic
        fun from(report: Report): ReportSimpleResponseDto {
            return ReportSimpleResponseDto(
                id = report.id ?: 0,
                status = report.status,
                reporterUserId = report.reporterUser.id ?: 0,
                targetUserId = report.targetUser.id ?: 0,
                createdDate = report.createdDate ?: LocalDateTime.MIN
            )
        }
    }
}