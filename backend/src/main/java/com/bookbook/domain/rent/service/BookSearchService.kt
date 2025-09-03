package com.bookbook.domain.rent.service

import com.bookbook.domain.rent.dto.response.BookSearchResponseDto
import com.bookbook.global.exception.ServiceException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class BookSearchService {

    @Value("\${aladin.ttbkey}")
    private lateinit var ttbKey: String

    private val restTemplate = RestTemplate()
    private val objectMapper = ObjectMapper()

    companion object {
        private val log = LoggerFactory.getLogger(BookSearchService::class.java)
    }

    // 알라딘 API를 호출해 책을 검색하고 결과를 리스트로 반환.
    fun searchBooks(query: String, start: Int): List<BookSearchResponseDto> {
        // 알라딘 API URL 생성
        val apiUrl = UriComponentsBuilder.fromHttpUrl("http://www.aladin.co.kr/ttb/api/ItemSearch.aspx")
            .queryParam("ttbkey", ttbKey)
            .queryParam("Query", query)
            .queryParam("QueryType", "Title")
            .queryParam("MaxResults", 10)
            .queryParam("start", start)
            .queryParam("SearchTarget", "Book")
            .queryParam("Output", "JS")
            .queryParam("Version", "20131101")
            .build()
            .toUriString()

        val jsonResponse = restTemplate.getForObject(apiUrl, String::class.java)

        val books = mutableListOf<BookSearchResponseDto>()
        try {
            val root = objectMapper.readTree(jsonResponse)
            val itemNode = root.path("item")

            if (itemNode.isArray) {
                for (item in itemNode) {
                    var title = item.path("title").asText("")
                    var author = item.path("author").asText("")
                    val publisher = item.path("publisher").asText("")
                    val pubDate = item.path("pubDate").asText("") // 필드명 수정: publishDate -> pubDate
                    val cover = item.path("cover").asText("")
                    val description = item.path("description").asText("") // description 필드 추가
                    val categoryName = item.path("categoryName").asText("") // categoryName 필드 추가

                    // 저자 정보 가공 : "지은이, 옮긴이, 감수 등 제거"
                    author = author.replace(Regex("\\(지은이\\)|\\(옮긴이\\)|\\(감수\\)"), "").trim()

                    // 카테고리 정보 가공: "국내도서>과학>기초과학/교양과학" -> "기초과학/교양과학" (가장 마지막 카테고리)
                    var category = ""
                    if (categoryName.isNotEmpty()) {
                        val categories = categoryName.split(">")
                        if (categories.isNotEmpty()) {
                            category = categories[categories.size - 1].trim()
                        }
                    }

                    // DTO 생성 시 추가된 필드들 포함
                    books.add(BookSearchResponseDto(title, author, publisher, pubDate, category, description, cover))
                }
            }
        } catch (e: Exception) {
            log.error("알라딘 API 응답 파싱 중 오류 발생", e)
            throw ServiceException("알라딘 API 응답 파싱 중 오류 발생: ${e.message}")
        }
        return books
    }

    // OCR을 위한 메서드 추가
    fun searchBooksByOcrTitle(detectedTitle: String, confidence: Double): List<BookSearchResponseDto> {
        // 1. 신뢰도에 따른 검색 전략 조정
        val maxResults = when {
            confidence >= 0.8 -> 5   // 높은 신뢰도: 적은 결과
            confidence >= 0.5 -> 10  // 중간 신뢰도: 보통 결과
            else -> 15               // 낮은 신뢰도: 많은 결과
        }

        // 2. 검색 쿼리 최적화 (노이즈 제거, 키워드 보정)
        val optimizedQuery = optimizeSearchQuery(detectedTitle)

        // 3. 알라딘 API 호출
        val searchResults = performAladinSearch(optimizedQuery, maxResults)

        // 4. 결과 후처리 (OCR 제목과의 유사도 기반 정렬)
        return rankSearchResults(searchResults, detectedTitle, confidence)
    }

    // 검색 쿼리 최적화 (OCR 노이즈 제거)
    private fun optimizeSearchQuery(title: String): String {
        var optimized = title

        // 특수문자 정리
        optimized = optimized.replace(Regex("[^가-힣a-zA-Z0-9\\s]"), " ")

        // 여러 공백을 하나로
        optimized = optimized.replace(Regex("\\s+"), " ").trim()

        // 너무 짧으면 그대로, 너무 길면 첫 부분만
        return when {
            optimized.length <= 2 -> title // 원본 유지
            optimized.length > 30 -> optimized.substring(0, 30) // 30자로 제한
            else -> optimized
        }
    }

    // 알라딘 API 호출 (기존 로직 재사용)
    private fun performAladinSearch(query: String, maxResults: Int): List<BookSearchResponseDto> {
        val apiUrl = UriComponentsBuilder.fromHttpUrl("http://www.aladin.co.kr/ttb/api/ItemSearch.aspx")
            .queryParam("ttbkey", ttbKey)
            .queryParam("Query", query)
            .queryParam("QueryType", "Title")
            .queryParam("MaxResults", maxResults)
            .queryParam("start", 1)
            .queryParam("SearchTarget", "Book")
            .queryParam("Output", "JS")
            .queryParam("Version", "20131101")
            .build()
            .toUriString()

        return try {
            val jsonResponse = restTemplate.getForObject(apiUrl, String::class.java)
            parseAladinResponse(jsonResponse ?: "")
        } catch (e: Exception) {
            log.error("알라딘 API 호출 실패: ${e.message}")
            emptyList()
        }
    }

    // 알라딘 응답 파싱 (기존 로직 재사용)
    private fun parseAladinResponse(jsonResponse: String): List<BookSearchResponseDto> {
        val books = mutableListOf<BookSearchResponseDto>()

        try {
            val root = objectMapper.readTree(jsonResponse)
            val itemNode = root.path("item")

            if (itemNode.isArray) {
                for (item in itemNode) {
                    var title = item.path("title").asText("")
                    var author = item.path("author").asText("")
                    val publisher = item.path("publisher").asText("")
                    val pubDate = item.path("pubDate").asText("")
                    val cover = item.path("cover").asText("")
                    val description = item.path("description").asText("")
                    val categoryName = item.path("categoryName").asText("")

                    // 기존 가공 로직 재사용
                    author = author.replace(Regex("\\(지은이\\)|\\(옮긴이\\)|\\(감수\\)"), "").trim()

                    var category = ""
                    if (categoryName.isNotEmpty()) {
                        val categories = categoryName.split(">")
                        if (categories.isNotEmpty()) {
                            category = categories[categories.size - 1].trim()
                        }
                    }

                    books.add(BookSearchResponseDto(title, author, publisher, pubDate, category, description, cover))
                }
            }
        } catch (e: Exception) {
            log.error("알라딘 API 응답 파싱 중 오류 발생", e)
        }

        return books
    }

    // 검색 결과 순위 조정 (OCR 제목과의 유사도 기반)
    private fun rankSearchResults(
        results: List<BookSearchResponseDto>,
        ocrTitle: String,
        confidence: Double
    ): List<BookSearchResponseDto> {
        if (results.isEmpty()) return results

        // 유사도 계산 및 정렬
        return results.map { book ->
            val similarity = calculateTitleSimilarity(ocrTitle, book.bookTitle)
            Pair(book, similarity)
        }
            .sortedByDescending { it.second } // 유사도 높은 순
            .map { it.first }
    }

    // 제목 유사도 계산 (간단한 Jaccard 유사도)
    private fun calculateTitleSimilarity(ocrTitle: String, bookTitle: String): Double {
        val ocrWords = ocrTitle.toLowerCase().split(Regex("\\s+")).toSet()
        val bookWords = bookTitle.toLowerCase().split(Regex("\\s+")).toSet()

        val intersection = ocrWords.intersect(bookWords).size
        val union = ocrWords.union(bookWords).size

        return if (union == 0) 0.0 else intersection.toDouble() / union
    }
}