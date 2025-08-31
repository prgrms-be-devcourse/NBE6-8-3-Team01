package com.bookbook.domain.suspend.dto.request

import jakarta.validation.constraints.*

// 25.08.28 김지훈

/**
 * 정지 요청 객체
 *
 * @param userId 정지시키고자 하는 유저의 id
 * @param reason 정지 사유
 * @param period 정지 기간 (일)
 */
data class UserSuspendRequestDto(
    @field:NotNull(message = "유저 ID는 필수입니다")
    @field:Min(value = 1, message = "유효한 유저 ID를 입력해주세요.")
    val userId: Long,

    @field:Size(min = 1, max = 300, message = "정지 사유는 300자를 넘을 수 없습니다")
    @field:Pattern(regexp = ".*\\S.*", message = "정지 사유를 입력해주세요.")
    val reason: String,

    @field:NotNull(message = "정지 기간은 필수입니다.")
    @field:Min(value = 3, message = "정지 기간은 3일 이상이어야 합니다.")
    @field:Max(value = 73000, message = "정지 기간은 200년을 넘을 수 없습니다.")
    val period: Int
)
