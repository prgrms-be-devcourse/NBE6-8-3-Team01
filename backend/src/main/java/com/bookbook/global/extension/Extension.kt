package com.bookbook.global.extension

import com.bookbook.domain.user.enums.Role
import com.bookbook.global.exception.ServiceException
import com.bookbook.global.security.CustomOAuth2User


fun CustomOAuth2User?.requireAdmin(): CustomOAuth2User {
    if (
        this == null ||
        this.userId == -1L ||
        this.role != Role.ADMIN
    ) throw ServiceException("401-1", "허가되지 않은 접근입니다.")

    return this
}