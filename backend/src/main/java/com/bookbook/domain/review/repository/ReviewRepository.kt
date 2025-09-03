package com.bookbook.domain.review.repository

import com.bookbook.domain.review.entity.Review
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ReviewRepository : JpaRepository<Review, Long> {
    fun findByRentIdAndReviewerId(rentId: Long, reviewerId: Long): Review?

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.revieweeId = :revieweeId")
    fun findAverageRatingByRevieweeId(@Param("revieweeId") revieweeId: Long): Double?

    // 특정 사용자에 대한 리뷰 개수 조회
    fun countByRevieweeId(revieweeId: Long): Long
}
