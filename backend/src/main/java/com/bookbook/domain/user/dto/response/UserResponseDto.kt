package com.bookbook.domain.user.dto.response

import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.enums.Role
import com.bookbook.domain.user.enums.UserStatus
import java.time.LocalDateTime

data class UserResponseDto(
    val id: Long,
    val username: String,
    val email: String?,
    val nickname: String?,
    val address: String?,
    val rating: Float,
    val role: Role,
    val userStatus: UserStatus,
    val createAt: LocalDateTime,
    val isRegistrationCompleted: Boolean
) {
    constructor(user: User) : this(
        id = user.id,
        username = user.username,
        email = user.email,
        nickname = user.nickname,
        address = user.address,
        rating = user.rating,
        role = user.role,
        userStatus = user.userStatus,
        createAt = user.createdDate,
        isRegistrationCompleted = user.isRegistrationCompleted()
    )
}