package com.bookbook.domain.chat.dto

import com.bookbook.domain.chat.entity.ChatRoom
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class ChatRoomResponse(
    val id: Long,
    val roomId: String,
    val rentId: Long,
    val bookTitle: String?,
    val bookImage: String?,
    val otherUserId: Long?,
    val otherUserNickname: String?,
    val otherUserProfileImage: String?,
    val lastMessage: String?,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val lastMessageTime: LocalDateTime?,

    val unreadCount: Long,
    val isActive: Boolean,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdDate: LocalDateTime
) {
    companion object {
        fun from(
            chatRoom: ChatRoom,
            bookTitle: String?,
            bookImage: String?,
            otherUserNickname: String?,
            otherUserProfileImage: String?,
            unreadCount: Long
        ): ChatRoomResponse {
            return ChatRoomResponse(
                id = chatRoom.id,
                roomId = chatRoom.roomId,
                rentId = chatRoom.rentId,
                bookTitle = bookTitle,
                bookImage = bookImage,
                otherUserId = null, // 컨트롤러에서 설정
                otherUserNickname = otherUserNickname,
                otherUserProfileImage = otherUserProfileImage,
                lastMessage = chatRoom.lastMessage,
                lastMessageTime = chatRoom.lastMessageTime,
                unreadCount = unreadCount,
                isActive = chatRoom.isActive,
                createdDate = chatRoom.createdDate
            )
        }
    }
}