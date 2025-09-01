package com.bookbook.domain.rent.dto.response

import com.bookbook.domain.rent.entity.RentStatus
import java.time.LocalDateTime

// 25.08.28 현준
// 대여 게시글의 상세 정보를 담는 DTO.
data class RentResponseDto(
    val id: Long,                          // 글 ID (BaseEntity.id는 Long 타입)
    val lenderUserId: Long?,               // 대여자 ID (nullable)
    val title: String?,                    // 글 제목 (nullable)
    val bookCondition: String?,            // 책 상태 (nullable)
    val bookImage: String?,                // 글쓴이가 올린 책 이미지 URL (nullable)
    val address: String?,                  // 사용자 주소 (nullable)
    val contents: String?,                 // 대여 내용 (nullable)
    val rentStatus: RentStatus?,           // 대여 상태 (nullable)

    // 책 정보
    val bookTitle: String?,                // 책 제목 (nullable)
    val author: String?,                   // 책 저자 (nullable)
    val publisher: String?,                // 책 출판사 (nullable)
    val category: String?,                 // 책 카테고리 (nullable)
    val description: String?,              // 책 설명 (nullable)
    val createdDate: LocalDateTime?,       // 생성일
    val modifiedDate: LocalDateTime?,      // 수정일

    // 글쓴이 정보
    val nickname: String?,                 // 글쓴이 닉네임 (nullable)
    val rating: Float?,                    // 글쓴이 평점
    val lenderPostCount: Int,              // 글쓴이 대여글 작성 수

    // 찜 상태 정보
    val isWishlisted: Boolean              // 현재 사용자의 찜 상태
)