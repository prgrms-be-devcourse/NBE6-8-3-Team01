package com.bookbook.domain.rentList.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "대여 신청 수락/거절 요청 DTO")
data class RentRequestDecisionDto(
    @Schema(description = "수락 여부 (true: 수락, false: 거절)", example = "true")
    @JsonProperty("approved")
    val approved: Boolean,
    
    @Schema(description = "거절 사유 (거절 시에만 필요)", example = "죄송합니다. 다른 분께 먼저 대여해드리기로 했습니다.")
    val rejectionReason: String? = null
)