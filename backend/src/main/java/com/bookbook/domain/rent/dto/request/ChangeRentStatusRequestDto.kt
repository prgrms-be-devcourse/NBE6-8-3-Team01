package com.bookbook.domain.rent.dto.request

import com.bookbook.domain.rent.entity.RentStatus

// 25.08.28 현준
// 대여 상태 변경을 위한 요청 DTO
data class ChangeRentStatusRequestDto(
    val status: RentStatus
)