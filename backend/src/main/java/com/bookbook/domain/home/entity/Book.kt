package com.bookbook.domain.home.entity

import jakarta.persistence.*

/**
 * 메인페이지용 Book 엔티티 (조회 전용)
 * 실제 rent 테이블을 조회하되, 필요한 필드만 매핑
 */
@Entity
@Table(name = "rent")
data class Book(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // 기존 Rent 엔티티와 동일한 필드명 사용 (bookImage)
    val bookImage: String? = null,

    val bookTitle: String? = null,

    val address: String? = null
) {
    // 프론트엔드 호환성을 위한 getter 메서드들
    val image: String?
        get() = bookImage
        
    val title: String?
        get() = bookTitle
        
    val region: String?
        get() = address
}
