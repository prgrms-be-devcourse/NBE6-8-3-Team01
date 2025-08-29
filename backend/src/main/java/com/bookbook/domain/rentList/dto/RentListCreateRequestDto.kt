package com.bookbook.domain.rentList.dto

import java.time.LocalDateTime

data class RentListCreateRequestDto(
    val loanDate: LocalDateTime,
    val rentId: Long
)