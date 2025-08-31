package com.bookbook.domain.rent.dto.response

import com.bookbook.domain.rent.entity.Rent
import org.springframework.data.domain.Page

// 대여 가능한 책 목록 조회 API 응답용 DTO
// 25.08.28 현준
data class RentAvailableResponseDto(
    val resultCode: String,
    val msg: String,
    val data: Data?,
    val success: Boolean
) {

    // 개별 책 정보 DTO - Kotlin Data Class로 변환 (Nullable 필드 처리)
    data class BookInfo(
        val id: Long?,
        val bookTitle: String?,       // 책 제목
        val author: String?,          // 저자
        val publisher: String?,       // 출판사
        val bookCondition: String?,   // 책 상태 (상, 중, 하)
        val bookImage: String?,       // 책 이미지 URL
        val address: String?,         // 위치 정보
        val category: String?,        // 카테고리
        val rentStatus: String?,      // 대여 상태 (대여가능, 대여중)
        val lenderUserId: Long?,      // 책 소유자 ID
        val lenderNickname: String?,  // 책 소유자 닉네임
        val title: String?,           // 대여글 제목
        val contents: String?,        // 대여 설명
        val createdDate: String?,     // 생성일
        val modifiedDate: String?     // 수정일
    ) {
        companion object {
            // Rent 엔티티와 User 엔티티로부터 BookInfo 생성 (Null Safe 처리)
            fun from(rent: Rent, lenderNickname: String?): BookInfo {
                return BookInfo(
                    id = rent.id,
                    bookTitle = rent.bookTitle,
                    author = rent.author,
                    publisher = rent.publisher,
                    bookCondition = rent.bookCondition,
                    bookImage = rent.bookImage,
                    address = rent.address,
                    category = rent.category,
                    rentStatus = rent.rentStatus.description,
                    lenderUserId = rent.lenderUserId,
                    lenderNickname = lenderNickname,
                    title = rent.title,
                    contents = rent.contents,
                    createdDate = rent.createdDate.toString(),
                    modifiedDate = rent.modifiedDate.toString()
                )
            }
        }
    }

    // 페이지네이션 정보 DTO - Kotlin Data Class로 변환
    data class PaginationInfo(
        val currentPage: Int,     // 현재 페이지 번호
        val totalPages: Int,      // 전체 페이지 수
        val totalElements: Long,  // 전체 요소 수
        val size: Int             // 페이지 크기
    ) {
        companion object {
            fun from(page: Page<*>): PaginationInfo {
                return PaginationInfo(
                    currentPage = page.number + 1,  // Spring Data JPA는 0부터 시작하므로 +1
                    totalPages = page.totalPages,
                    totalElements = page.totalElements,
                    size = page.size
                )
            }
        }
    }

    // 실제 API 응답 데이터 - Kotlin Data Class로 변환
    data class Data(
        val books: List<BookInfo>,
        val pagination: PaginationInfo
    )

    companion object {
        // 성공 응답 생성 - Named Parameters 활용
        fun success(books: List<BookInfo>, pagination: PaginationInfo): RentAvailableResponseDto {
            return RentAvailableResponseDto(
                resultCode = "200",
                msg = "대여 가능한 책 목록 조회 성공",
                data = Data(
                    books = books,
                    pagination = pagination
                ),
                success = true
            )
        }

        // 빈 결과 응답 생성 - Default Parameters 활용
        fun empty(): RentAvailableResponseDto {
            return RentAvailableResponseDto(
                resultCode = "200",
                msg = "검색 조건에 맞는 책이 없습니다",
                data = Data(
                    books = emptyList(),
                    pagination = PaginationInfo(
                        currentPage = 1,
                        totalPages = 1,
                        totalElements = 0,
                        size = 12
                    )
                ),
                success = true
            )
        }
    }
}