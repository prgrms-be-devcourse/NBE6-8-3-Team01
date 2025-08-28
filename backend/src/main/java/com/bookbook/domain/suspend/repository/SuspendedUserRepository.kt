package com.bookbook.domain.suspend.repository

import com.bookbook.domain.suspend.entity.SuspendedUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

// 25.08.28 김지훈

@Repository
interface SuspendedUserRepository : JpaRepository<SuspendedUser, Long> {
    @Query("""
        SELECT s FROM SuspendedUser s WHERE
        (:userId IS NULL OR s.user.id = :userId)
        ORDER BY s.suspendedAt DESC
    """)
    fun findAllFilteredUser(
        pageable: Pageable,
        @Param("userId") userId: Long?
    ): Page<SuspendedUser>
}
