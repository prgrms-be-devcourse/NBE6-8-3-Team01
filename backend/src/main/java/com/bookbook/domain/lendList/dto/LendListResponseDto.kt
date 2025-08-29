package com.bookbook.domain.lendList.dto

import com.bookbook.domain.rent.entity.Rent
import java.time.LocalDateTime

data class LendListResponseDto(
    val id: Long,
    val lenderUserId: Long?,
    val title: String?,
    val bookTitle: String?,
    val author: String?,
    val publisher: String?,
    val bookCondition: String?,
    val bookImage: String?,
    val address: String?,
    val rentStatus: String?,
    val borrowerNickname: String?,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime,
    val returnDate: LocalDateTime?,
    val hasReview: Boolean
) {
    constructor(
        rent: Rent,
        borrowerNickname: String?,
        returnDate: LocalDateTime?,
        hasReview: Boolean
    ) : this(
        id = rent.id,
        lenderUserId = rent.lenderUserId,
        title = rent.title,
        bookTitle = rent.bookTitle,
        author = rent.author,
        publisher = rent.publisher,
        bookCondition = rent.bookCondition,
        bookImage = rent.bookImage,
        address = rent.address,
        rentStatus = rent.rentStatus?.name,
        borrowerNickname = borrowerNickname,
        createdDate = rent.createdDate,
        modifiedDate = rent.modifiedDate,
        returnDate = returnDate,
        hasReview = hasReview
    )
}