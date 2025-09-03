package com.bookbook.domain.rent.dto.request

// 09.01 양현준
data class ImageOcrRequestDto(
    val imageFile: String, // 이미지 파일 경로
    val processType: String = "BOOK_TITLE"  // OCR 처리 타입
)