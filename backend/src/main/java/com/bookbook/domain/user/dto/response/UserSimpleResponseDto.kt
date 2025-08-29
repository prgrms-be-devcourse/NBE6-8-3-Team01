package com.bookbook.domain.user.dto.response

import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.enums.UserStatus
import java.time.LocalDateTime

data class UserSimpleResponseDto(
    val id: Long,
    val username: String,
    val nickname: String,
    val email: String,
    val rating: Float,
    val userStatus: UserStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
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
    )
}