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
    constructor(report: Report) : this(
        id = report.id,
        status = report.status,
        reporterUserId = report.reporterUserId,
        targetUserId = report.targetUserId,
        createdDate = report.createdDate
    )
}