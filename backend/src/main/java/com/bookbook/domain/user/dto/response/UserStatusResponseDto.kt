package com.bookbook.domain.user.dto.response

import com.bookbook.domain.user.enums.UserStatus

data class UserStatusResponseDto(
    var id: Long,
    var userStatus: UserStatus
)