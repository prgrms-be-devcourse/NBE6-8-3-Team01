package com.bookbook.domain.rentList.dto

import com.bookbook.domain.rentList.entity.RentList
import java.time.LocalDateTime

data class RentListResponseDto(
    // Rent 엔티티에서 모든 필드들이 nullable인 상태임
    val id: Long,
    val loanDate: LocalDateTime,
    val returnDate: LocalDateTime,
    val borrowerUserId: Long,
    val rentId: Long,
    val title: String?,
    val bookTitle: String?,
    val author: String?,
    val publisher: String?,
    val bookCondition: String?,
    val bookImage: String?,
    val rentStatus: String,
    val address: String?,
    val lenderNickname: String?,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime,
    val hasReview: Boolean
) {
    constructor(
        rentList: RentList, 
        lenderNickname: String?, 
        hasReview: Boolean
    ) : this(
        id = rentList.id,
        loanDate = rentList.loanDate,
        returnDate = rentList.returnDate,
        borrowerUserId = rentList.borrowerUser.id,
        rentId = rentList.rent.id,
        title = rentList.rent.title,
        bookTitle = rentList.rent.bookTitle,
        author = rentList.rent.author,
        publisher = rentList.rent.publisher,
        bookCondition = rentList.rent.bookCondition,
        bookImage = rentList.rent.bookImage,
        rentStatus = rentList.status.name,
        address = rentList.rent.address,
        lenderNickname = lenderNickname,
        createdDate = rentList.createdDate,
        modifiedDate = rentList.modifiedDate,
        hasReview = hasReview
    )
}