package com.bookbook.domain.rent.repository

import com.bookbook.domain.rent.entity.Rent
import com.bookbook.domain.rent.entity.RentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

// 대여 게시글 관련 데이터베이스 작업을 처리하는 리포지토리 인터페이스
// 25.08.28 현준
@Repository
interface RentRepository : JpaRepository<Rent, Long> {

    // 대여자가 작성한 글 갯수 조회
    fun countByLenderUserId(lenderUserId: Long): Int

    // 대여 가능한 책 목록 조회 (기본)
    fun findByRentStatus(rentStatus: RentStatus, pageable: Pageable): Page<Rent>

    // 지역 필터링이 포함된 대여 가능한 책 목록 조회
    fun findByRentStatusAndAddressContaining(
        rentStatus: RentStatus,
        address: String,
        pageable: Pageable
    ): Page<Rent>

    // 카테고리 필터링이 포함된 대여 가능한 책 목록 조회
    fun findByRentStatusAndCategoryContaining(
        rentStatus: RentStatus,
        category: String,
        pageable: Pageable
    ): Page<Rent>

    // 검색어 필터링이 포함된 대여 가능한 책 목록 조회 (책 제목, 저자, 출판사에서 검색)
    @Query(
        "SELECT r FROM Rent r WHERE r.rentStatus = :rentStatus AND r.address LIKE %:address% AND " +
                "(r.bookTitle LIKE %:searchKeyword% OR r.author LIKE %:searchKeyword% OR r.publisher LIKE %:searchKeyword%)"
    )
    fun findByRentStatusAndSearchKeyword(
        @Param("rentStatus") rentStatus: RentStatus,
        @Param("searchKeyword") searchKeyword: String,
        pageable: Pageable
    ): Page<Rent>

    // 지역 + 카테고리 필터링
    fun findByRentStatusAndAddressContainingAndCategoryContaining(
        rentStatus: RentStatus,
        address: String,
        category: String,
        pageable: Pageable
    ): Page<Rent>

    // 지역 + 검색어 필터링
    @Query(
        "SELECT r FROM Rent r WHERE r.rentStatus = :rentStatus AND r.address LIKE %:address% AND " +
                "(r.bookTitle LIKE %:searchKeyword% OR r.author LIKE %:searchKeyword% OR r.publisher LIKE %:searchKeyword%)"
    )
    fun findByRentStatusAndAddressAndSearchKeyword(
        @Param("rentStatus") rentStatus: RentStatus,
        @Param("address") address: String,
        @Param("searchKeyword") searchKeyword: String,
        pageable: Pageable
    ): Page<Rent>

    // 카테고리 + 검색어 필터링
    @Query(
        "SELECT r FROM Rent r WHERE r.rentStatus = :rentStatus AND r.category LIKE %:category% AND " +
                "(r.bookTitle LIKE %:searchKeyword% OR r.author LIKE %:searchKeyword% OR r.publisher LIKE %:searchKeyword%)"
    )
    fun findByRentStatusAndCategoryAndSearchKeyword(
        @Param("rentStatus") rentStatus: RentStatus,
        @Param("category") category: String,
        @Param("searchKeyword") searchKeyword: String,
        pageable: Pageable
    ): Page<Rent>

    // 모든 필터링 조건 (지역 + 카테고리 + 검색어)
    @Query(
        "SELECT r FROM Rent r WHERE r.rentStatus = :rentStatus AND r.address LIKE %:address% AND r.category LIKE %:category% AND " +
                "(r.bookTitle LIKE %:searchKeyword% OR r.author LIKE %:searchKeyword% OR r.publisher LIKE %:searchKeyword%)"
    )
    fun findByRentStatusAndAddressAndCategoryAndSearchKeyword(
        @Param("rentStatus") rentStatus: RentStatus,
        @Param("address") address: String,
        @Param("category") category: String,
        @Param("searchKeyword") searchKeyword: String,
        pageable: Pageable
    ): Page<Rent>

    // 조건에 따른 게시글 내역 검색
    // 대여 내역 상태 or 대여 게시글 작성자
    @Query("""
        SELECT r FROM Rent r WHERE
        (:userId IS NULL OR :userId = r.lenderUserId) AND
        (:status IS NULL OR r.rentStatus in :status)
        ORDER BY r.createdDate DESC
    """)
    fun findFilteredRentHistory(
        pageable: Pageable,
        @Param("status") status: List<RentStatus>?,
        @Param("userId") userId: Long?
    ): Page<Rent>
}