package com.bookbook.domain.wishList.repository

import com.bookbook.domain.wishList.entity.WishList
import com.bookbook.domain.wishList.enums.WishListStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WishListRepository : JpaRepository<WishList, Long> {
    fun findByUserIdAndStatusOrderByCreatedDateDesc(userId: Long, status: WishListStatus): List<WishList>

    fun findByUserIdAndRentIdAndStatus(userId: Long, rentId: Long, status: WishListStatus): WishList?
}
