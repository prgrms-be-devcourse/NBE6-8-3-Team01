package com.bookbook.domain.rentBookList.dto

import com.bookbook.domain.rent.entity.Rent
import java.time.LocalDateTime

// 책 대여 목록 응답 DTO
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
    // Rent 엔티티로부터 DTO 생성하는 생성자
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
        // 이미지 URL 처리 메서드
        private fun processImageUrl(imageUrl: String?): String {
            if (imageUrl.isNullOrBlank()) {
                return "/book-placeholder.png"
            }

            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                return imageUrl
            }

            if (imageUrl.startsWith("/uploads/")) {
                return "http://localhost:8080$imageUrl"
            }

            if (imageUrl.startsWith("uploads/")) {
                return "http://localhost:8080/$imageUrl"
            }

            return "http://localhost:8080${if (imageUrl.startsWith("/")) imageUrl else "/$imageUrl"}"
        }
    }
}
