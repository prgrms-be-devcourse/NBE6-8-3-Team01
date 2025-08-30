package com.bookbook.domain.chat.dto

import com.bookbook.domain.chat.entity.ChatMessage
import com.bookbook.domain.chat.enums.MessageType
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class MessageResponse(
    val id: Long,
    val roomId: String,
    val senderId: Long,
    val senderNickname: String?,
    val senderProfileImage: String?,
    val content: String,
    val messageType: MessageType,
    val isRead: Boolean,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val readTime: LocalDateTime?,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdDate: LocalDateTime,

    @JsonProperty("isMine") // JSON 직렬화 시 필드명 명시
    val isMine: Boolean // 내가 보낸 메시지인지 여부
) {
    companion object {
        fun from(
            message: ChatMessage,
            senderNickname: String?,
            senderProfileImage: String?,
            isMine: Boolean
        ): MessageResponse {
            return MessageResponse(
                id = message.id,
                roomId = message.roomId,
                senderId = message.senderId,
                senderNickname = senderNickname,
                senderProfileImage = senderProfileImage,
                content = message.content,
                messageType = message.messageType,
                isRead = message.isRead,
                readTime = message.readTime,
                createdDate = message.createdDate,
                isMine = isMine
            )
        }
    }
}