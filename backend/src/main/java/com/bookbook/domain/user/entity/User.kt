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

    // Lombok의 `builder`를 모방한 빌더 패턴 구현
    companion object {
        @JvmStatic
        fun builder(): UserBuilder {
            return UserBuilder()
        }
    }

    data class UserBuilder(
        private var id: Long? = null,
        private var username: String? = null,
        private var password: String? = null,
        private var nickname: String? = null,
        private var email: String? = null,
        private var address: String? = null,
        private var rating: Float? = null,
        private var role: Role? = null,
        private var userStatus: UserStatus? = null,
        private var createAt: LocalDateTime? = null,
        private var updateAt: LocalDateTime? = null,
        private var registrationCompleted: Boolean? = null
    ) {
        fun id(id: Long?) = apply { this.id = id }
        fun username(username: String) = apply { this.username = username }
        fun password(password: String) = apply { this.password = password }
        fun nickname(nickname: String?) = apply { this.nickname = nickname }
        fun email(email: String?) = apply { this.email = email }
        fun address(address: String?) = apply { this.address = address }
        fun rating(rating: Float?) = apply { this.rating = rating }
        fun role(role: Role?) = apply { this.role = role }
        fun userStatus(userStatus: UserStatus?) = apply { this.userStatus = userStatus }
        fun createAt(createAt: LocalDateTime?) = apply { this.createAt = createAt }
        fun updateAt(updateAt: LocalDateTime?) = apply { this.updateAt = updateAt }
        fun registrationCompleted(registrationCompleted: Boolean?) = apply { this.registrationCompleted = registrationCompleted }

        fun build(): User {
            return User(
                id = id,
                username = username!!,
                password = password!!,
                nickname = nickname,
                email = email,
                address = address,
                rating = rating ?: 0.0f,
                role = role ?: Role.USER,
                userStatus = userStatus ?: UserStatus.ACTIVE,
                createAt = createAt,
                updateAt = updateAt,
                registrationCompleted = registrationCompleted ?: false
            )
        }
    }
}