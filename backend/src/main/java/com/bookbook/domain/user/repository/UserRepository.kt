package com.bookbook.domain.user.repository

import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.enums.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>

    @Query("""
        SELECT u FROM User u WHERE 
        u.userStatus = com.bookbook.domain.user.enums.UserStatus.SUSPENDED AND 
        u.resumedAt IS NOT NULL AND
        u.resumedAt <= NOW() ORDER BY 
        u.resumedAt ASC LIMIT 100
    """)
    fun findAllPossibleResumedUsers(): List<User>

    @Query(
        """
        SELECT u FROM User u WHERE
        (:status IS NULL OR u.userStatus in :status) AND
        (:userId IS NULL OR u.id = :userId)
        ORDER BY u.createdDate DESC
    """
    )
    fun findFilteredUsers(
        pageable: Pageable?,
        @Param("status") status: List<UserStatus>?,
        @Param("userId") userId: Long?
    ): Page<User>

    fun existsByUsername(username: String): Boolean
    fun existsByNickname(nickname: String): Boolean
}