package com.bookbook.domain.notification.entity

import com.bookbook.domain.notification.enums.NotificationType
import com.bookbook.domain.user.entity.User
import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

//08-29 유효상
@Entity
class Notification() : BaseEntity() {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    var receiver: User? = null
        protected set // 알림을 받는 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    var sender: User? = null
        protected set // 알림을 발생시킨 사용자 (요청자)

    @Enumerated(EnumType.STRING)
    var type: NotificationType? = null
        protected set // 알림 타입

    var title: String? = null
        protected set // "도서 대여 요청이 도착했어요!"

    var message: String? = null
        protected set // "대여 부탁드립니다!"

    var bookTitle: String? = null
        protected set // 관련 도서 제목

    var bookImageUrl: String? = null
        protected set // 도서 이미지 URL

    var relatedId: Long? = null
        protected set // 관련 데이터 ID (rent_id, wishlist_id 등)

    var isRead: Boolean = false
        protected set // 읽음 여부

    var isProcessed: Boolean = false
        protected set // 처리 완료 여부 (수락/거절 완료 시 true)



    constructor(
        receiver: User,
        sender: User?,
        type: NotificationType,
        title: String?,
        message: String?,
        bookTitle: String?,
        bookImageUrl: String?,
        relatedId: Long?
    ) : this() {
        this.receiver = receiver
        this.sender = sender
        this.type = type
        this.title = title
        this.message = message
        this.bookTitle = bookTitle
        this.bookImageUrl = bookImageUrl
        this.relatedId = relatedId
        this.isRead = false
        this.isProcessed = false
    }

    // 읽음 처리 - 예외 없이 자연스럽게 처리
    fun markAsRead() {
        this.isRead = true
    }

    // 처리 완료 표시 (수락/거절 완료 시)
    fun markAsProcessed() {
        this.isProcessed = true
    }

    companion object {
        // 알림 생성 시 기본값 설정
        fun createNotification(
            receiver: User,
            sender: User?,
            type: NotificationType,
            message: String?,
            bookTitle: String?,
            bookImageUrl: String?,
            relatedId: Long?
        ): Notification {
            requireNotNull(receiver) { "알림을 받을 사용자가 필요합니다." }
            requireNotNull(type) { "알림 타입이 필요합니다." }

            return Notification(
                receiver = receiver,
                sender = sender,
                type = type,
                title = type.defaultTitle,
                message = message,
                bookTitle = bookTitle,
                bookImageUrl = bookImageUrl,
                relatedId = relatedId
            )
        }
    }
}
