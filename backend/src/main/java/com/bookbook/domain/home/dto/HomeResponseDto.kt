package com.bookbook.domain.home.dto

/**
 * 메인페이지 응답 DTO
 */
data class HomeResponseDto(
    /**
     * 지역 정보 (현재는 "전체"로 고정)
     */
    val region: String? = null,

    /**
     * 도서 이미지 URL 목록 (최대 5개)
     */
    val bookImages: List<String> = emptyList(),

    /**
     * 이미지가 있는 전체 도서 개수
     */
    val totalBooksInRegion: Long? = null,

    /**
     * 사용자 지역 (IP 기반 또는 세션 기반)
     */
    val userRegion: String? = null
) {
    /**
     * 응답 메시지 (지역에 따라 동적 생성)
     */
    val message: String
        get() = if (region == "전체") {
            "최근 등록된 도서"
        } else {
            "${region}의 최근 등록된 도서"
        }
}
