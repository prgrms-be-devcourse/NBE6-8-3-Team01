package com.bookbook.domain.rent.controller

import com.bookbook.domain.rent.dto.response.ImageOcrResponseDto
import com.bookbook.domain.rent.service.BookTitleParsingService
import com.bookbook.domain.rent.service.ImageOcrService
import io.swagger.v3.oas.annotations.Operation
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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
    fun testOcr(@RequestParam("file") file: MultipartFile): ResponseEntity<*> {

        if (file.isEmpty) {
            return ResponseEntity.badRequest().body("파일이 비어있습니다.")
        }

        return try {
            // 파일 검증
            imageOcrService.validateImageFile(file)

            // OCR 처리
            val extractedText = imageOcrService.extractTextFromImage(file)

            // 책 제목 추출
            val (detectedTitle, confidence) = bookTitleParsingService.extractBookTitle(extractedText)

            val response = ImageOcrResponseDto(
                success = true,
                extractedText = extractedText,
                detectedBookTitle = detectedTitle,
                confidence = confidence,
                searchResults = null, // 다음 Task에서 구현
                message = if (detectedTitle != null) "책 제목이 감지되었습니다." else "책 제목을 감지하지 못했습니다."
            )

            ResponseEntity.ok(response)

        } catch (e: IllegalArgumentException) {
            log.error("파일 검증 실패: {}", e.message)
            ResponseEntity.badRequest().body("파일 검증 실패: ${e.message}")
        } catch (e: Exception) {
            log.error("OCR 처리 중 오류 발생: {}", e.message, e)
            val response = ImageOcrResponseDto(
                success = false,
                extractedText = "",
                detectedBookTitle = null,
                confidence = 0.0,
                searchResults = null,
                message = "OCR 처리 중 오류가 발생했습니다: ${e.message}"
            )
            ResponseEntity.status(500).body(response)
        }
    }

}