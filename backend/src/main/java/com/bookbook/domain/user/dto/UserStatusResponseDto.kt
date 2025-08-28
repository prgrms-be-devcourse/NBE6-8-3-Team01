package com.bookbook.domain.user.dto

import com.bookbook.domain.user.enums.UserStatus

data class UserStatusResponseDto(
    var id: Long? = null,
    var userStatus: UserStatus? = null
)