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
    companion object {
        @JvmStatic
        fun from(wishList: WishList, lenderNickname: String): WishListResponseDto {
            val rent = wishList.rent!!
            return WishListResponseDto(
                id = wishList.id!!,
                rentId = rent.id,
                title = rent.title,
                bookTitle = rent.bookTitle,
                author = rent.author,
                publisher = rent.publisher,
                bookCondition = rent.bookCondition,
                rentStatus = rent.rentStatus!!.name,
                bookImage = rent.bookImage,
                address = rent.address,
                lenderNickname = lenderNickname,
                createDate = wishList.createdDate!!
            )
        }
    }
}