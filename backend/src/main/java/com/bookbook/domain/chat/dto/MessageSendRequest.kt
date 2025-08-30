package com.bookbook.domain.chat.dto

import com.bookbook.domain.chat.enums.MessageType
import jakarta.validation.constraints.NotBlank

data class MessageSendRequest(
    @field:NotBlank(message = "채팅방 ID는 필수입니다.")
    val roomId: String,

    @field:NotBlank(message = "메시지 내용은 필수입니다.")
    val content: String,

    val messageType: MessageType = MessageType.TEXT
)