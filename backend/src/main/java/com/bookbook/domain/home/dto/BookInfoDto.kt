package com.bookbook.domain.home.dto

/**
 * 도서 정보 DTO (ID 포함)
 */
data class BookInfoDto(
    /**
     * 도서 ID
     */
    val id: Long? = null,

    /**
     * 도서 이미지 URL
     */
    val imageUrl: String? = null,

    /**
     * 도서 제목
     */
    val title: String? = null,

    /**
     * 도서 제목 (중복이지만 프론트엔드 호환성을 위해 추가)
     */
    val bookTitle: String? = null
)
