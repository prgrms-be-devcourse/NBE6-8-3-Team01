package com.bookbook.domain.wishList.entity

import com.bookbook.domain.rent.entity.Rent
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.wishList.enums.WishListStatus
import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
class WishList(
    @ManyToOne
    var user: User? = null, // 찜한 사용자 (위시리스트의 주인)

    @ManyToOne
    var rent: Rent? = null, // 찜한 도서 (대여 게시글)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: WishListStatus = WishListStatus.ACTIVE
) : BaseEntity()
