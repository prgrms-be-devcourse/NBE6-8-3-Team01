package com.bookbook.domain.rent.service

import com.bookbook.domain.rent.dto.BookSearchResponseDto
import com.fasterxml.jackson.databind.ObjectMapper
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
            e.printStackTrace() // 스택 트레이스 출력
            throw RuntimeException("알라딘 API 응답 파싱 중 오류 발생: ${e.message}", e) // 더 명확한 메시지
        }
        return books
    }
}
