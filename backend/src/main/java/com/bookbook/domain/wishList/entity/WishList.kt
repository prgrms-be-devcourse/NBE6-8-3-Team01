package com.bookbook.domain.wishList.entity

import com.bookbook.domain.rent.entity.Rent
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.wishList.enums.WishListStatus
import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
class WishList(
    @ManyToOne(fetch = FetchType.LAZY)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    val rent: Rent,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: WishListStatus = WishListStatus.ACTIVE
) : BaseEntity()
