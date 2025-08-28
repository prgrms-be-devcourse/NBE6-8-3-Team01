package com.bookbook.domain.user.dto

data class UserLoginRequestDto(
    var username: String? = null,
    var password: String? = null
)