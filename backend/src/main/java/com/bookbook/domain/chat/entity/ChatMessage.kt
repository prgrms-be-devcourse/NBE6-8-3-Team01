package com.bookbook.domain.chat.entity

import com.bookbook.domain.chat.enums.MessageType
import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "chat_messages")
class ChatMessage(
    @Column(name = "room_id", nullable = false)
    val roomId: String,

    @Column(name = "sender_id", nullable = false)
    val senderId: Long,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    val messageType: MessageType = MessageType.TEXT,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "read_time")
    var readTime: LocalDateTime? = null

) : BaseEntity()
