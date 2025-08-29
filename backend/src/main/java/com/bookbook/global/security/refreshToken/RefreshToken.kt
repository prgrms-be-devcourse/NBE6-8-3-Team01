package com.bookbook.global.security.refreshToken

import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.Entity
import java.time.LocalDateTime

@Entity
class RefreshToken(
    var token: String,
    val userId: Long,
    var expiryDate: LocalDateTime
) : BaseEntity() {

    // JPA 사용을 위한 기본 생성자
    constructor() : this("", 0L, LocalDateTime.now())

    fun updateToken(newToken: String, newExpiryDate: LocalDateTime) {
        this.token = newToken
        this.expiryDate = newExpiryDate
    }

    fun isExpired(): Boolean {
        return this.expiryDate.isBefore(LocalDateTime.now())
    }
}