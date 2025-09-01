package com.bookbook.domain.rent.dto.response

import com.bookbook.domain.rent.entity.Rent
import com.bookbook.domain.rent.entity.RentStatus
import java.time.LocalDateTime

// 25.08.28 현준
// 대여 게시글의 상세 정보를 담는 DTO.
data class RentDetailResponseDto(
    val id: Long,                           // 글 ID
    val lenderUserId: Long,               // 대여자 ID
    val title: String,                    // 글 제목
    val bookCondition: String,            // 책 상태
    val bookImage: String,                // 글쓴이가 올린 책 이미지 URL
    val address: String,                  // 사용자 주소
    val contents: String,                 // 대여 내용
    val rentStatus: RentStatus,           // 대여 상태

    // 책 정보
    val bookTitle: String,                // 책 제목
    val author: String,                   // 책 저자
    val publisher: String,                // 책 출판사
    val category: String,                 // 책 카테고리
    val description: String,              // 책 설명
    val createdDate: LocalDateTime,       // 생성일
    val modifiedDate: LocalDateTime       // 수정일
) {
    constructor(rent: Rent): this(
        id = rent.id,
        lenderUserId = rent.lenderUserId,
        title = rent.title,
        bookCondition = rent.bookCondition,
        bookImage = rent.bookImage,
        address = rent.address,
        contents = rent.contents,
        rentStatus = rent.rentStatus,
        bookTitle = rent.bookTitle,
        author = rent.author,
        publisher = rent.publisher,
        category = rent.category,
        description = rent.description,
        createdDate = rent.createdDate,
        modifiedDate = rent.modifiedDate
    )
}