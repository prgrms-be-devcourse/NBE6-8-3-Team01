package com.bookbook.domain.user.dto.request

import jakarta.validation.constraints.NotBlank

data class UserCreateRequestDto(
    @field:NotBlank(message = "아이디는 필수 입력값입니다.")
    var nickname: String,
    @field:NotBlank(message = "주소는 필수 입력값입니다.")
    var address: String
)