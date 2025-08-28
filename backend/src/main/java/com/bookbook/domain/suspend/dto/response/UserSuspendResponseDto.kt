package com.bookbook.domain.suspend.dto.response

import com.bookbook.domain.suspend.entity.SuspendedUser
import java.time.LocalDateTime

// 25.08.28 김지훈

/**
 * 정지 유저 정보 응답 객체
 *
 * @param id 객체 id
 * @param userId 정지된 유저의 id
 * @param name 유저명 (username)
 * @param reason 정지 사유
 * @param suspendedAt 정지 일시
 * @param resumedAt 정지 해제 일시
 */
data class UserSuspendResponseDto(
    val id: Long,
    val userId: Long,
    val name: String,
    val reason: String,
    val suspendedAt: LocalDateTime,
    val resumedAt: LocalDateTime
) {
    constructor(suspendedUser: SuspendedUser) : this(
        id = suspendedUser.id,
        userId = suspendedUser.user.id,
        name = suspendedUser.user.username,
        reason = suspendedUser.reason,
        suspendedAt = suspendedUser.suspendedAt,
        resumedAt = suspendedUser.resumedAt
    )
}