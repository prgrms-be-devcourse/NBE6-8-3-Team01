package com.bookbook.domain.rent.dto.response

import com.bookbook.domain.rent.entity.Rent
import com.bookbook.domain.rent.entity.RentStatus
import java.time.LocalDateTime

// 25.08.28 현준
// 대여 게시글의 간단한 정보를 담는 DTO.
data class RentSimpleResponseDto(
    val id: Long,
    val lenderUserId: Long?,
    val status: RentStatus?,
    val bookCondition: String?,
    val bookTitle: String?,
    val author: String?,
    val publisher: String?,
    val createdDate: LocalDateTime?,
    val modifiedDate: LocalDateTime?
) {
    companion object {
        // Rent Entity로부터 DTO 생성하는 factory 메서드
        fun from(rent: Rent): RentSimpleResponseDto {
            return RentSimpleResponseDto(
                id = rent.id,
                lenderUserId = rent.lenderUserId,
                status = rent.rentStatus,
                bookCondition = rent.bookCondition,
                bookTitle = rent.bookTitle,
                author = rent.author,
                publisher = rent.publisher,
                createdDate = rent.createdDate,
                modifiedDate = rent.modifiedDate
            )
        }
    }
}