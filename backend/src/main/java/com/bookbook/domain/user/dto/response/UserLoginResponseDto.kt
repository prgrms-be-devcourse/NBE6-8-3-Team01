package com.bookbook.domain.user.dto.response

import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.enums.Role
import com.bookbook.domain.user.enums.UserStatus

data class UserLoginResponseDto(
    val id: Long,
    val username: String,
    val nickname: String,
    val role: Role,
    val status: UserStatus
) {
    constructor(user: User) : this(
        id = user.id,
        username = user.username,
        nickname = user.nickname ?: "정보 없음",
        role = user.role,
        status = user.userStatus
    )
}
