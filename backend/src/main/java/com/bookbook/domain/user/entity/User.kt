package com.bookbook.domain.user.entity

import com.bookbook.domain.suspend.entity.SuspendedUser
import com.bookbook.domain.user.enums.Role
import com.bookbook.domain.user.enums.UserStatus
import com.bookbook.global.entity.BaseEntity
import jakarta.persistence.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
class User(
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

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

    @Min(0)
    @Max(5)
    @Column(name = "rating", nullable = false)
    var rating: Float = 0.0f,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: Role = Role.USER,

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    var userStatus: UserStatus = UserStatus.ACTIVE,

    @CreatedDate
    var createAt: LocalDateTime? = null,

    @LastModifiedDate
    var updateAt: LocalDateTime? = null,

    var suspendedAt: LocalDateTime? = null,

    var resumedAt: LocalDateTime? = null,

    @Column(name = "registration_completed", nullable = false)
    var registrationCompleted: Boolean = false,
) : BaseEntity() {

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL])
    val suspends: MutableList<SuspendedUser> = mutableListOf()

    fun changeUserStatus(userStatus: UserStatus) {
        this.userStatus = userStatus
    }

    fun changeRating(rating: Float) {
        if (rating < 0 || rating > 5) {
            throw IllegalArgumentException("Rating must be between 0 and 5")
        }
        this.rating = rating
    }

    fun suspend(periodDays: Int) {
        this.userStatus = UserStatus.SUSPENDED
        this.suspendedAt = LocalDateTime.now()
        this.resumedAt = suspendedAt?.plusDays(periodDays.toLong())
    }

    fun resume() {
        this.userStatus = UserStatus.ACTIVE
        this.suspendedAt = null
        this.resumedAt = null
    }

    fun isSuspended(): Boolean {
        return userStatus == UserStatus.SUSPENDED
    }

    fun changeUsername(username: String) {
        this.username = username
    }

    // 자바 코드와의 호환성을 위한 수동 구현
    fun isRegistrationCompleted(): Boolean {
        return this.registrationCompleted
    }

}