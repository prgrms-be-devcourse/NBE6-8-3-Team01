package com.bookbook.domain.rent.service

import com.bookbook.domain.rent.dto.response.ImageOcrResponseDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

// 09.01 양현준
// OCR + 알라딘 검색 통합 서비스
@Service
class OcrBookSearchService(
    private val imageOcrService: ImageOcrService,
    private val bookTitleParsingService: BookTitleParsingService,
    private val bookSearchService: BookSearchService
) {

    companion object {
        private val log = LoggerFactory.getLogger(OcrBookSearchService::class.java)
    }

    fun processImageAndSearch(imageFile: MultipartFile): ImageOcrResponseDto {

        // 1단계: 파일 검증
        imageOcrService.validateImageFile(imageFile)

        // 2단계: OCR 처리
        val extractedText = imageOcrService.extractTextFromImage(imageFile)

        // 3단계: 책 제목 추출
        val (detectedTitle, confidence) = bookTitleParsingService.extractBookTitle(extractedText)

        // 4단계: 알라딘 검색 (제목이 감지된 경우만)
        val searchResults = if (detectedTitle != null) {
            try {
                bookSearchService.searchBooksByOcrTitle(detectedTitle, confidence)
            } catch (e: Exception) {
                log.warn("알라딘 검색 실패, 빈 결과 반환: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }

        return ImageOcrResponseDto(
            extractedText = extractedText, // OCR로 추출된 전체 텍스트
            detectedBookTitle = detectedTitle, // 파싱된 책 제목
            confidence = confidence, // 신뢰도 (0.0 ~ 1.0)
            searchResults = searchResults // 알라딘 검색 결과
        )
    }
}