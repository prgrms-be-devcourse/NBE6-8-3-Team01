// TODO: MessageType enum을 Kotlin으로 변환 후 import 경로 확인 필요
package com.bookbook.domain.chat.entity

import com.bookbook.domain.chat.enums.MessageType
import com.bookbook.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "chat_message")
class ChatMessage : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

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

    @Column(nullable = false)
    var createdDate: LocalDateTime = LocalDateTime.now()

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