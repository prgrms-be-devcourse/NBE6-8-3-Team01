package com.bookbook.domain.wishList.dto

import com.bookbook.domain.wishList.entity.WishList
import java.time.LocalDateTime

data class WishListResponseDto(
    val id: Long,
    val rentId: Long,
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
        rentId = wishList.rent.id,
        title = wishList.rent.title,
        bookTitle = wishList.rent.bookTitle,
        author = wishList.rent.author,
        publisher = wishList.rent.publisher,
        bookCondition = wishList.rent.bookCondition,
        rentStatus = wishList.rent.rentStatus.name,
        bookImage = wishList.rent.bookImage,
        address = wishList.rent.address,
        lenderNickname = lenderNickname,
        createDate = wishList.createdDate ?: LocalDateTime.now()
    )
}