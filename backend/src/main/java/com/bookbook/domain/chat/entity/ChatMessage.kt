package com.bookbook.domain.chat.entity

import com.bookbook.domain.chat.enums.MessageType
import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "chat_message")
class ChatMessage : BaseEntity() {
    @Column(nullable = false)
    var roomId: String = ""

    @Column(nullable = false)
    var senderId: Long = 0L

    @Column(columnDefinition = "TEXT")
    var content: String = ""

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var messageType: MessageType = MessageType.TEXT

    @Column(nullable = false)
    var isRead: Boolean = false

    var readTime: LocalDateTime? = null

    // 메시지 읽음 처리
    fun markAsRead() {
        this.isRead = true
        this.readTime = LocalDateTime.now()
    }

    // 내가 보낸 메시지인지 확인
    fun isSentBy(userId: Int?): Boolean {
        return userId?.let { senderId == it.toLong() } ?: false
    }
}