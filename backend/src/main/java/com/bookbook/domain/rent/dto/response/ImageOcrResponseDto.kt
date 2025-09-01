package com.bookbook.domain.rent.dto.response

data class ImageOcrResponseDto(
    val success: Boolean,
    val extractedText: String,           // OCR로 추출된 전체 텍스트
    val detectedBookTitle: String?,      // 파싱된 책 제목
    val confidence: Double,              // 신뢰도 (0.0 ~ 1.0)
    val searchResults: List<BookSearchResponseDto>?, // 알라딘 검색 결과
    val message: String                  // 사용자에게 보여줄 메시지
)