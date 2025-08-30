package com.bookbook.domain.chat.dto

import jakarta.validation.constraints.NotNull

data class ChatRoomCreateRequest(
    @field:NotNull(message = "대여 게시글 ID는 필수입니다.")
    val rentId: Long,

    @field:NotNull(message = "빌려주는 사람 ID는 필수입니다.")
    val lenderId: Long
)