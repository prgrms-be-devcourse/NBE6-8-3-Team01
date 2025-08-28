package com.bookbook.domain.suspend.entity

import com.bookbook.domain.user.entity.User
import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

// 25.08.28 김지훈

@Entity
@Table(name = "suspended_users")
@EntityListeners(AuditingEntityListener::class)
class SuspendedUser(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User,

    @Column(name = "suspend_reason", nullable = false)
    var reason: String,

    @Column(name = "suspended_at", nullable = false)
    var suspendedAt: LocalDateTime,

    @Column(name = "resumed_at", nullable = false)
    var resumedAt: LocalDateTime
) : BaseEntity() {
    constructor(user: User, reason: String) : this(
        user = user,
        reason = reason,
        suspendedAt = user.suspendedAt!!,
        resumedAt = user.resumedAt!!
    )
}
