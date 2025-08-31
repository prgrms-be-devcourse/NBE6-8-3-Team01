package com.bookbook.domain.rent.controller

import com.bookbook.domain.rent.dto.response.BookSearchResponseDto
import com.bookbook.domain.rent.service.BookSearchService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 25.08.29 현준
// 책 검색 및 이미지 업로드 API 컨트롤러
@RestController
@RequestMapping("/api/v1/bookbook")
class BookSearchController(
    private val bookSearchService: BookSearchService
) {

    @GetMapping("/searchbook")
    @Operation(summary = "알라딘 API를 이용한 책 검색")
    fun searchBooks(
        @RequestParam query: String,
        @RequestParam(defaultValue = "1") start: Int
    ): ResponseEntity<List<BookSearchResponseDto>> {
        return try {
            val searchResults = bookSearchService.searchBooks(query, start)
            if (searchResults.isEmpty()) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.ok(searchResults)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.status(500).body(null)
        }
    }
}