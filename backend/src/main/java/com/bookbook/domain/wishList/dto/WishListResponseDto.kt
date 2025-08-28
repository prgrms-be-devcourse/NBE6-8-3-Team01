package com.bookbook.domain.wishList.dto

import com.bookbook.domain.wishList.entity.WishList
import java.time.LocalDateTime

data class WishListResponseDto(
    val id: Long,
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
        // TODO: Rent 도메인이 코틀린으로 마이그레이션되면 다음으로 교체:
//        rentId = wishList.rent.id,
//        title = wishList.rent.title,
//        bookTitle = wishList.rent.bookTitle,
//        author = wishList.rent.author,
//        publisher = wishList.rent.publisher,
//        bookCondition = wishList.rent.bookCondition,
//        rentStatus = wishList.rent.rentStatus.name,
//        bookImage = wishList.rent.bookImage,
//        address = wishList.rent.address,
        rentId = 0,
        title = "",
        bookTitle = "",
        author = "",
        publisher = "",
        bookCondition = "",
        rentStatus = "",
        bookImage = "",
        address = "",
        lenderNickname = lenderNickname,
        createDate = wishList.createdDate ?: LocalDateTime.now()
    )
}