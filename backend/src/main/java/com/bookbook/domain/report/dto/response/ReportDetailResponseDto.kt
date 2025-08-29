package com.bookbook.domain.report.dto.response

import com.bookbook.domain.report.entity.Report
import com.bookbook.domain.report.enums.ReportStatus
import java.time.LocalDateTime

data class ReportDetailResponseDto(
    val id: Long,
    val status: ReportStatus,
    val reporterUserId: Long,
    val targetUserId: Long,
    val closerId: Long?,
    val reason: String,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime,
    val reviewedDate: LocalDateTime
) {
    constructor(report: Report) : this(
        id = report.id,
        status = report.status,
        reporterUserId = report.reporterUserId,
        targetUserId = report.targetUserId,
        closerId = report.closerId,
        reason = report.reason,
        createdDate = report.createdDate,
        modifiedDate = report.modifiedDate,
        reviewedDate = report.reviewedDate
    )
}