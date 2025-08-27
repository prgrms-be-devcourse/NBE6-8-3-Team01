package com.bookbook.global.jpa.dto.response

import org.springframework.data.domain.Page

/**
 * 페이지 응답 정보를 담고 있습니다
 * @param content 응답 데이터 본문
 * @param pageInfo 페이지 세부 정보
 */
@JvmRecord
data class PageResponseDto<T>(
    val content: List<T>,
    val pageInfo: PageInfo
) {
    constructor(page: Page<T>) : this (
        page.content,
        PageInfo(
            page.number + 1,
            page.totalPages,
            page.totalElements,
            page.numberOfElements,
            page.size
        )
    )
}


/**
 * 페이지 세부 정보 입니다.
 *
 * @param currentPage 현재 페이지
 * @param totalPages 전체 페이지 수
 * @param totalElements 전체 요소의 갯수
 * @param currentPageElements 현재 페이지의 요소 갯수
 * @param size 한 페이지 당 크기
 */
@JvmRecord
data class PageInfo(
    val currentPage: Int,
    val totalPages: Int,
    val totalElements: Long,
    val currentPageElements: Int,
    val size: Int
)