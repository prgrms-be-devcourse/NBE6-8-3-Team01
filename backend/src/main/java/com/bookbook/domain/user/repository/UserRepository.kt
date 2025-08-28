package com.bookbook.domain.user.repository

import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.enums.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface UserRepository : JpaRepository<User?, Long?> {
    fun findByNickname(nickname: String?): Optional<User?>?
    fun findByUsername(username: String?): Optional<User?>?
    fun findByEmail(email: String?): Optional<User?>?
    fun findAllByUserStatusAndResumedAtBefore(userStatus: UserStatus?, now: LocalDateTime?): MutableList<User?>?

    @Query(
        """
        SELECT u FROM User u WHERE
        (:status IS NULL OR u.userStatus in :status) AND
        (:userId IS NULL OR u.id = :userId)
        ORDER BY u.createAt DESC
    """
    )
    fun findFilteredUsers(
        pageable: Pageable?,
        @Param("status") status: MutableList<UserStatus?>?,
        @Param("userId") userId: Long?
    ): Page<User?>?

    fun existsByUsername(username: String?): Boolean // 사용자명 중복확인
    fun existsByNickname(nickname: String?): Boolean // 닉네임 중복확인
}
