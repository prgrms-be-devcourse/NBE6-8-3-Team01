package com.bookbook.domain.review.dto

import com.bookbook.domain.review.entity.Review
import java.time.LocalDateTime

data class ReviewResponseDto(
    val id: Long,
    val rentId: Long,
    val reviewerId: Long,
    val revieweeId: Long,
    val reviewerNickname: String?,
    val revieweeNickname: String?,
    val bookTitle: String?,
    val bookImage: String?,
    val rating: Int,
    val reviewType: String,
    val createdDate: LocalDateTime?
) {
    constructor(
        review: Review,
        reviewerNickname: String?,
        revieweeNickname: String?,
        bookTitle: String?,
        bookImage: String?
    ) : this(
        id = review.id,
        rentId = review.rentId,
        reviewerId = review.reviewerId,
        revieweeId = review.revieweeId,
        reviewerNickname = reviewerNickname,
        revieweeNickname = revieweeNickname,
        bookTitle = bookTitle,
        bookImage = bookImage,
        rating = review.rating,
        reviewType = review.reviewType,
        createdDate = review.createdDate
    )
}