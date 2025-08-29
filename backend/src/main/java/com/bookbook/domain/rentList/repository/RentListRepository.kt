package com.bookbook.domain.rentList.repository

import com.bookbook.domain.rentList.entity.RentList
import com.bookbook.domain.rentList.entity.RentRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RentListRepository : JpaRepository<RentList, Long> {
    fun findByBorrowerUserIdOrderByCreatedDateDesc(borrowerUserId: Long): List<RentList>

    fun findByRentId(rentId: Long): List<RentList>

    fun findByRentIdAndStatus(rentId: Long, status: RentRequestStatus): List<RentList>

    // 특정 신청자의 특정 책에 대한 특정 상태의 신청 조회
    fun findByRentIdAndBorrowerUserIdAndStatus(
        rentId: Long,
        borrowerUserId: Long,
        status: RentRequestStatus
    ): List<RentList>

    // 특정 신청자의 특정 책에 대한 모든 신청 조회 (상태 무관)
    fun findByRentIdAndBorrowerUserId(rentId: Long, borrowerUserId: Long): List<RentList>

    // 중복 신청 방지를 위한 메서드
    fun existsByBorrowerUserIdAndRentIdAndStatus(
        borrowerUserId: Long,
        rentId: Long,
        status: RentRequestStatus
    ): Boolean
}