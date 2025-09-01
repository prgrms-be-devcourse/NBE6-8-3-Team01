package com.bookbook.domain.user.dto.request

import org.hibernate.validator.constraints.Length

data class UserLoginRequestDto(
    @field:Length(min = 2, max = 30) var username: String,
    @field:Length(min = 2, max = 30) var password: String
)