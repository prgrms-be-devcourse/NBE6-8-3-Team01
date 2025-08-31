package com.bookbook.domain.user.dto.response

import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.enums.Role
import com.bookbook.domain.user.enums.UserStatus
import java.time.LocalDateTime

data class UserDetailResponseDto(
    val id: Long,
    val username: String,
    val nickname: String,
    val email: String,
    val rating: Float,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val userStatus: UserStatus,
    val role: Role,
    val address: String,
    val suspendCount: Int,
    val suspendedAt: LocalDateTime?,
    val resumedAt: LocalDateTime?
) {
    constructor(user: User): this(
        id = user.id,
        username = user.username,
        nickname = user.nickname ?: "정보 없음",
        email = user.email ?: "정보 없음",
        rating = user.rating,
        createdAt = user.createdDate,
        updatedAt = user.modifiedDate,
        userStatus = user.userStatus,
        role = user.role,
        address = user.address ?: "정보 없음",
        suspendCount = user.suspends.size,
        suspendedAt = user.suspendedAt,
        resumedAt = user.resumedAt,
    )
}