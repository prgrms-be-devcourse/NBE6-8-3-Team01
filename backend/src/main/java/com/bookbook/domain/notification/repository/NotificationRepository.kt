package com.bookbook.domain.notification.repository

import com.bookbook.domain.notification.entity.Notification
import com.bookbook.domain.notification.enums.NotificationType
import com.bookbook.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

//08-29 유효상
interface NotificationRepository : JpaRepository<Notification, Long> {
    // 사용자별 알림 조회 (최신순)
    fun findByReceiverOrderByCreatedDateDesc(receiver: User): List<Notification>

    // 사용자별 읽지 않은 알림 개수
    fun countByReceiverAndIsReadFalse(receiver: User): Long

    // 사용자별 읽지 않은 알림 조회
    fun findByReceiverAndIsReadFalseOrderByCreatedDateDesc(receiver: User): List<Notification>

    // 사용자의 모든 알림을 읽음 처리하는 쿼리
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver = :receiver AND n.isRead = false")
    fun markAllAsReadByReceiver(@Param("receiver") receiver: User)

    // 특정 수신자, 타입, 관련 ID로 알림 조회
    fun findByReceiverAndTypeAndRelatedId(receiver: User, type: NotificationType, relatedId: Long): List<Notification>
}
