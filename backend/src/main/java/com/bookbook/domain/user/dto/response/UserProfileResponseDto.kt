package com.bookbook.domain.user.dto.response

import com.bookbook.domain.user.entity.User

data class UserProfileResponseDto(
    val userId: Long,
    val nickname: String?,
    val mannerScore: Double,
    val mannerScoreCount: Int
) {
    constructor(user: User, mannerScore: Double, mannerScoreCount: Int) : this(
        userId = user.id,
        nickname = user.nickname,
        mannerScore = mannerScore,
        mannerScoreCount = mannerScoreCount
    )
}