package com.bookbook.domain.report.dto.response

import com.bookbook.domain.report.entity.Report
import com.bookbook.domain.report.enums.ReportStatus
import java.time.LocalDateTime

data class ReportDetailResponseDto(
    @JvmField
    val id: Long,
    val status: ReportStatus,
    val reporterUserId: Long,
    val targetUserId: Long,
    val closerId: Long?,
    val reason: String,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime,
    val reviewedDate: LocalDateTime?
) {
    companion object {
        @JvmStatic
        fun from(report: Report): ReportDetailResponseDto {
            val closerId = report.closer?.id

            return ReportDetailResponseDto(
                id = report.id ?: 0,
                status = report.status,
                reporterUserId = report.reporterUser.id ?: 0,
                targetUserId = report.targetUser.id ?: 0,
                closerId = closerId,
                reason = report.reason,
                createdDate = report.createdDate ?: LocalDateTime.MIN,
                modifiedDate = report.modifiedDate ?: LocalDateTime.MIN,
                reviewedDate = report.reviewedDate
            )
        }
    }
}