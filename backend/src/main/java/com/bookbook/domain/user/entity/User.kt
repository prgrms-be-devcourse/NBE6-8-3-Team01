package com.bookbook.domain.user.entity

import com.bookbook.domain.suspend.entity.SuspendedUser
import com.bookbook.domain.user.enums.Role
import com.bookbook.domain.user.enums.UserStatus
import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
@AttributeOverride(name = "id", column = Column(name = "user_id"))
class User(
    @Column(name = "username", unique = true, nullable = false)
    var username: String,

    @Column(name = "password", nullable = false)
    var password: String,

    @Column(name = "nickname", unique = true, nullable = true)
    var nickname: String? = null,

    @Column(name = "email", unique = true, nullable = true)
    var email: String? = null,

    @Column(name = "address", nullable = true)
    var address: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: Role = Role.USER,

    @Column(name = "registration_completed", nullable = false)
    var registrationCompleted: Boolean = false
) : BaseEntity() {

    @Column(name = "rating", nullable = false)
    var rating: Float = 0.0f

    @Column(name = "suspended_at", nullable = true)
    var suspendedAt: LocalDateTime? = null

    @Column(name = "resumed_at", nullable = true)
    var resumedAt: LocalDateTime? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    var userStatus: UserStatus = UserStatus.ACTIVE

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL])
    val suspends: MutableList<SuspendedUser> = mutableListOf()

    fun changeRating(rating: Float) {
        if (rating !in 0.0..5.0) {
            throw IllegalArgumentException("Rating must be between 0 and 5")
        }
        this.rating = rating
    }

    fun suspend(periodDays: Int) {
        val now = LocalDateTime.now()

        this.userStatus = UserStatus.SUSPENDED
        this.suspendedAt = now
        this.resumedAt = now.plusDays(periodDays.toLong())
    }

    fun resume() {
        this.userStatus = UserStatus.ACTIVE
        this.suspendedAt = null
        this.resumedAt = null
    }

    val isSuspended: Boolean
            get() = this.userStatus == UserStatus.SUSPENDED

    val isAdmin: Boolean
        get() = this.role == Role.ADMIN

    // 자바 코드와의 호환성을 위한 수동 구현
    fun isRegistrationCompleted(): Boolean {
        return this.registrationCompleted
    }
}