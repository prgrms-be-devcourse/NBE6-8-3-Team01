package com.bookbook.domain.rentList.entity

import com.bookbook.domain.rent.entity.Rent
import com.bookbook.domain.user.entity.User
import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class RentList(
    @ManyToOne
    var borrowerUser: User? = null,
    
    @ManyToOne
    var rent: Rent? = null,
    
    var loanDate: LocalDateTime? = null,
    var returnDate: LocalDateTime? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RentRequestStatus = RentRequestStatus.PENDING
) : BaseEntity()