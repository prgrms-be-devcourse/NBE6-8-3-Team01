package com.bookbook.domain.review.entity

import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.Entity

@Entity
class Review(   // JPA 엔티티지만 비즈니스 로직상 리뷰는 수정되지 않으므로 불변으로 처리
    // 대여 게시글 ID
    val rentId: Long,
    
    // 리뷰 작성자 ID - 리뷰를 쓴 사람
    val reviewerId: Long,
    
    // 리뷰 대상자 ID - 리뷰를 받은 사람
    val revieweeId: Long,
    
    // 평점 - 1~5점 사이의 정수값
    val rating: Int,
    
    // 리뷰 타입 - 어떤 방향의 리뷰인지 구분
    // "LENDER_TO_BORROWER": 대여자 → 대여받은 사람
    // "BORROWER_TO_LENDER": 대여받은 사람 → 대여자
    val reviewType: String
) : BaseEntity()