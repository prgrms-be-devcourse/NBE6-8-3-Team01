package com.bookbook.domain.report.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ReportRequestDto(

    @field:NotNull(message = "신고 대상 사용자의 ID는 필수입니다.")
    val targetUserId: Long,

    @field:NotBlank(message = "신고 사유는 비워둘 수 없습니다.")
    val reason: String
)