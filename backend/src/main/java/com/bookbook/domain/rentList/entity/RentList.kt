package com.bookbook.domain.rentList.entity

import com.bookbook.domain.rent.entity.Rent
import com.bookbook.domain.user.entity.User
import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class RentList(
    var loanDate: LocalDateTime,
    var returnDate: LocalDateTime,
    
    @Enumerated(EnumType.STRING)
    var status: RentRequestStatus = RentRequestStatus.PENDING
) : BaseEntity() {
    
    @ManyToOne
    lateinit var borrowerUser: User
    
    @ManyToOne  
    lateinit var rent: Rent
}