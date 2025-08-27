package com.bookbook.global.jpa.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이지 응답 정보를 담고 있습니다
 * @param content 응답 데이터 본문
 * @param pageInfo 페이지 세부 정보
*/
public record PageResponseDto<T> (
    List<T> content,
    PageInfo pageInfo
){
    /**
     * 페이지 세부 정보 입니다.
     *
     * @param currentPage 현재 페이지
     * @param totalPages 전체 페이지 수
     * @param totalElements 전체 요소의 갯수
     * @param currentPageElements 현재 페이지의 요소 갯수
     * @param size 한 페이지 당 크기
     */
    record PageInfo(
            Integer currentPage,
            Integer totalPages,
            Long totalElements,
            Integer currentPageElements,
            Integer size
    ) {
    }

    public static <T> PageResponseDto<T> from(Page<T> page) {
        PageInfo pageInfo = new PageInfo(
                page.getNumber() + 1,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumberOfElements(),
                page.getSize()
        );

        return new PageResponseDto<>(page.getContent(), pageInfo);
    }
}