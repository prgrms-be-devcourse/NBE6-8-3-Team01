package com.bookbook.domain.rentBookList.dto

import com.bookbook.domain.rent.entity.Rent
import java.time.LocalDateTime

data class RentBookListResponseDto(
    val id: Long,
    val bookTitle: String?,
    val author: String?,
    val publisher: String?,
    val bookCondition: String?,
    val bookImage: String,
    val address: String?,
    val category: String?,
    val rentStatus: String,
    val lenderUserId: Long?,
    val lenderNickname: String?,
    val title: String?,
    val contents: String?,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime
) {
    constructor(rent: Rent, lenderNickname: String?) : this(
        id = rent.id,
        bookTitle = rent.bookTitle,
        author = rent.author,
        publisher = rent.publisher,
        bookCondition = rent.bookCondition,
        bookImage = processImageUrl(rent.bookImage),
        address = rent.address,
        category = rent.category,
        rentStatus = rent.rentStatus?.description ?: "",
        lenderUserId = rent.lenderUserId,
        lenderNickname = lenderNickname,
        title = rent.title,
        contents = rent.contents,
        createdDate = rent.createdDate,
        modifiedDate = rent.modifiedDate
    )

    companion object {
        private fun processImageUrl(imageUrl: String?): String {
            if (imageUrl.isNullOrBlank()) {
                return "/book-placeholder.png"
            }

            // 이미 전체 URL인 경우 그대로 반환
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                return imageUrl
            }

            // 상대 경로인 경우 절대 경로로 변환
            if (imageUrl.startsWith("/uploads/")) {
                return "http://localhost:8080$imageUrl"
            }

            // uploads/로 시작하는 경우
            if (imageUrl.startsWith("uploads/")) {
                return "http://localhost:8080/$imageUrl"
            }

            // 기타 경우 기본 처리
            return "http://localhost:8080${if (imageUrl.startsWith("/")) imageUrl else "/$imageUrl"}"
        }
    }
}
