package com.bookbook.domain.rent.controller

import com.bookbook.domain.rent.dto.response.ImageOcrResponseDto
import com.bookbook.domain.rent.service.ImageOcrService
import com.bookbook.domain.rent.service.BookTitleParsingService
import com.bookbook.global.rsdata.RsData
import io.swagger.v3.oas.annotations.Operation
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

// 09.01. 양현준
@RestController
@RequestMapping("/api/v1/bookbook")
class ImageOcrController(
    private val imageOcrService: ImageOcrService,
    private val bookTitleParsingService: BookTitleParsingService
) {
    
    private val log = LoggerFactory.getLogger(ImageOcrController::class.java)

    @PostMapping("/ocr-test")
    @Operation(summary = "OCR 테스트 - 이미지에서 텍스트 추출")
    fun testOcr(@RequestParam("file") file: MultipartFile): RsData<ImageOcrResponseDto?> {
        
        // 파일 검증 (ServiceException 발생 시 GlobalExceptionHandler에서 처리)
        imageOcrService.validateImageFile(file)
        
        // OCR 처리
        val extractedText = imageOcrService.extractTextFromImage(file)
        
        // 책 제목 추출
        val (detectedTitle, confidence) = bookTitleParsingService.extractBookTitle(extractedText)
        
        val responseData = ImageOcrResponseDto(
            extractedText = extractedText,
            detectedBookTitle = detectedTitle,
            confidence = confidence,
            searchResults = null // Task 3.3에서 구현
        )
        
        val message = if (detectedTitle != null) {
            "책 제목이 감지되었습니다: $detectedTitle (신뢰도: ${String.format("%.1f", confidence * 100)}%)"
        } else {
            "책 제목을 감지하지 못했습니다. (신뢰도: ${String.format("%.1f", confidence * 100)}%)"
        }
        
        return RsData.of("200-1", message, responseData)
    }
}