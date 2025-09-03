package com.bookbook.domain.rent.controller

import com.bookbook.domain.rent.dto.response.ImageOcrResponseDto
import com.bookbook.domain.rent.service.BookTitleParsingService
import com.bookbook.domain.rent.service.ImageOcrService
import com.bookbook.domain.rent.service.OcrBookSearchService
import com.bookbook.global.rsdata.RsData
import io.swagger.v3.oas.annotations.Operation
import org.slf4j.LoggerFactory
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
    private val bookTitleParsingService: BookTitleParsingService,
    private val ocrBookSearchService: OcrBookSearchService
) {
    private val log = LoggerFactory.getLogger(ImageOcrController::class.java)

    // 통합 OCR + 알라딘 검색 엔드포인트
    @PostMapping("/ocr-book-search")
    @Operation(summary = "이미지 OCR + 도서 검색 통합",
        description = "책 표지 이미지를 업로드하면 OCR로 제목을 추출하고 알라딘 API에서 자동 검색")
    fun ocrBookSearch(@RequestParam("file") file: MultipartFile): RsData<ImageOcrResponseDto?> {

        log.info("통합 OCR + 도서 검색 요청, 파일: ${file.originalFilename}")

        // 통합 서비스로 모든 처리 위임
        val result = ocrBookSearchService.processImageAndSearch(file)

        // 성공 메시지 생성
        val message = when {
            result.searchResults?.isNotEmpty() == true -> {
                "책을 찾았습니다! '${result.detectedBookTitle}' 검색 결과 ${result.searchResults.size}건"
            }
            result.detectedBookTitle != null -> {
                "책 제목은 감지했지만 검색 결과가 없습니다: '${result.detectedBookTitle}'"
            }
            else -> {
                "책 제목을 감지하지 못했습니다. 수동으로 검색해주세요."
            }
        }

        return RsData("200-1", message, result)
    }
}