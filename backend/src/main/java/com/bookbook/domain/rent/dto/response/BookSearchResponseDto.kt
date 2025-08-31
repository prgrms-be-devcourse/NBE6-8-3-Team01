package com.bookbook.domain.rent.dto.response

// 25.08.28 현준
// Aladin API의 응답 데이터를 기반으로 필요한 필드만 추출하여 정의
data class BookSearchResponseDto(
    val bookTitle: String,       // 책 제목 (Aladin API 'title' 매핑)
    val author: String,          // 저자 (Aladin API 'author' 매핑)
    val publisher: String,       // 출판사 (Aladin API 'publisher' 매핑)
    val pubDate: String,         // 출판 날짜 (Aladin API 'pubDate' 매핑)
    val category: String,        // 카테고리 (Aladin API 'categoryName' 파싱)
    val bookDescription: String, // 책 설명 (Aladin API 'description' 매핑)
    val coverImageUrl: String    // 책 표지 이미지 URL (Aladin API 'cover' 매핑)
)