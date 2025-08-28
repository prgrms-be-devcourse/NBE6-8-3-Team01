package com.bookbook.domain.suspend.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

// 25.08.28 김지훈

/**
 * 정지 요청 객체
 *
 * @param userId 정지시키고자 하는 유저의 id
 * @param reason 정지 사유
 * @param period 정지 기간 (일)
 */
data class UserSuspendRequestDto(
    @field:NotNull val userId: Long,

    @field:NotBlank val reason: String,

    @field:NotNull val period: Int
)
