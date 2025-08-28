package com.bookbook.domain.rentBookList.repository

import com.bookbook.domain.rent.entity.Rent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RentBookListRepository : JpaRepository<Rent, Long> {
    
    @Query(
        """SELECT r FROM Rent r WHERE 
           (:region IS NULL OR r.address LIKE %:region%) AND 
           (:category IS NULL OR r.category = :category) AND 
           (:search IS NULL OR r.bookTitle LIKE %:search% OR r.author LIKE %:search% OR r.publisher LIKE %:search%) AND 
           r.rentStatus = com.bookbook.domain.rent.entity.RentStatus.AVAILABLE 
           ORDER BY r.createdDate DESC"""
    )
    fun findAvailableBooks(
        @Param("region") region: String?,
        @Param("category") category: String?,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<Rent>

    @Query(
        """SELECT r FROM Rent r WHERE 
           r.rentStatus = com.bookbook.domain.rent.entity.RentStatus.AVAILABLE 
           ORDER BY r.createdDate DESC"""
    )
    fun findAllAvailableBooks(pageable: Pageable): Page<Rent>

    @Query("SELECT DISTINCT r.address FROM Rent r WHERE r.address IS NOT NULL ORDER BY r.address")
    fun findDistinctRegions(): List<String>

    @Query("SELECT DISTINCT r.category FROM Rent r WHERE r.category IS NOT NULL ORDER BY r.category")
    fun findDistinctCategories(): List<String>
}
