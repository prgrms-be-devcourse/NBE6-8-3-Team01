package com.bookbook.domain.user.dto

import com.bookbook.domain.user.entity.User

data class UserProfileResponseDto(
    val userId: Long,
    val nickname: String?,
    val mannerScore: Double,
    val mannerScoreCount: Int
) {
    companion object {
        @JvmStatic
        fun from(user: User, mannerScore: Double, mannerScoreCount: Int): UserProfileResponseDto {
            return UserProfileResponseDto(
                userId = user.id!!,
                nickname = user.nickname,
                mannerScore = mannerScore,
                mannerScoreCount = mannerScoreCount
            )
        }
    }
}