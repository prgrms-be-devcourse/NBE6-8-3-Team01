package com.bookbook.domain.rent.dto.response

// OCR 응답 DTO - RsData<ImageOcrResponseDto> 형태로 래핑되어 응답
data class ImageOcrResponseDto(
    val extractedText: String,                       // OCR로 추출된 전체 텍스트
    val detectedBookTitle: String?,                  // 파싱된 책 제목
    val confidence: Double,                          // 신뢰도 (0.0 ~ 1.0)
    val searchResults: List<BookSearchResponseDto>?  // 알라딘 검색 결과
)