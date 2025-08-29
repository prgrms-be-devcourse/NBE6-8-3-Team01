package com.bookbook.global.security.refreshToken

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
    fun findByUserId(userId: Long): RefreshToken?

    @Transactional
    fun deleteByUserId(userId: Long)
}