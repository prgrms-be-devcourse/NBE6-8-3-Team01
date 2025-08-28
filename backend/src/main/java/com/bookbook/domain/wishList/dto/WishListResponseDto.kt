package com.bookbook.domain.wishList.dto

import com.bookbook.domain.wishList.entity.WishList
import java.time.LocalDateTime

data class WishListResponseDto(
    val id: Int,
    val rentId: Int,
    val title: String,
    val bookTitle: String,
    val author: String,
    val publisher: String,
    val bookCondition: String,
    val rentStatus: String,
    val bookImage: String,
    val address: String,
    val lenderNickname: String,
    val createDate: LocalDateTime
) {
    constructor(wishList: WishList, lenderNickname: String) : this(
        id = wishList.id,
        // Rent가 코틀린으로 마이그레이션되면 null-safe 처리 불필요
        rentId = wishList.rent?.id ?: 0,
        title = wishList.rent?.title ?: "",
        bookTitle = wishList.rent?.bookTitle ?: "",
        author = wishList.rent?.author ?: "",
        publisher = wishList.rent?.publisher ?: "",
        bookCondition = wishList.rent?.bookCondition ?: "",
        rentStatus = wishList.rent?.rentStatus?.name ?: "",
        bookImage = wishList.rent?.bookImage ?: "",
        address = wishList.rent?.address ?: "",
        lenderNickname = lenderNickname,
        createDate = wishList.createdDate ?: LocalDateTime.now()
    )
}