package com.bookbook.domain.rent.dto.response

import com.bookbook.domain.rent.entity.Rent
import java.nio.file.Files
import java.nio.file.Paths

// 25.08.28 현준
// 대여 가능한 책 목록 조회 API 응답용 DTO
// 프론트엔드 BookCard 컴포넌트에 맞춰 설계됨
data class RentListResponseDto(
    // 기본 정보 (Nullable 필드 처리)
    val id: Long?,                // Rent ID (프론트: id)
    val bookTitle: String?,       // 책 제목 (프론트: bookTitle)
    val author: String?,          // 저자 (프론트: author)
    val publisher: String?,       // 출판사 (프론트: publisher)
    val bookCondition: String?,   // 책 상태 (프론트: bookCondition)
    val bookImage: String?,       // 책 이미지 URL (프론트: bookImage)
    val address: String?,         // 위치 정보 (프론트: address)
    val category: String?,        // 카테고리 (프론트: category)
    val rentStatus: String?,      // 대여 상태 (프론트: rentStatus)
    val lenderUserId: Long?,      // 책 소유자 ID (프론트: lenderUserId)

    // 상세 정보 (카드에서는 표시 안하지만 필요시 사용)
    val title: String?,           // 대여글 제목 (프론트: title?)
    val contents: String?         // 대여 설명 (프론트: contents?)
) {
    companion object {
        /**
         * Rent 엔티티를 RentListResponseDto로 변환
         * Named Parameters와 Extension Properties 활용
         */
        fun from(rent: Rent): RentListResponseDto {
            return RentListResponseDto(
                id = rent.id?.toLong(), // Int? → Long? 변환
                bookTitle = rent.bookTitle,
                author = rent.author,
                publisher = rent.publisher,
                bookCondition = rent.bookCondition,
                bookImage = rent.bookImage.normalizeImageUrl(),
                address = rent.address,
                category = rent.category,
                rentStatus = rent.rentStatus?.description,
                lenderUserId = rent.lenderUserId,
                title = rent.title,
                contents = rent.contents
            )
        }

        /**
         * 이미지 URL 정규화 및 검증을 위한 Extension Function
         * - 상대 경로를 절대 경로로 변환
         * - uploads 경로 정규화
         * - 파일 존재 여부 확인 (선택적)
         */
        private fun String?.normalizeImageUrl(): String {
            if (this.isNullOrEmpty()) {
                return "/book-placeholder.png" // 기본 이미지
            }

            // 이미 완전한 URL인 경우 그대로 반환
            if (this.startsWith("http://") || this.startsWith("https://")) {
                return this
            }

            // 상대 경로인 경우 절대 경로로 변환
            if (this.startsWith("/")) {
                return this // 프론트에서 처리하도록 상대 경로 유지
            }

            // 그 외의 경우 uploads 경로로 처리
            return "/uploads/$this"
        }

        /**
         * 파일 존재 여부 확인 (개발용)
         * 실제 운영에서는 성능상 사용하지 않는 것을 권장
         */
        private fun String.fileExists(): Boolean {
            return try {
                if (this.startsWith("/uploads/")) {
                    val fileName = this.substring("/uploads/".length)
                    val filePath = Paths.get("uploads", fileName)
                    Files.exists(filePath)
                } else {
                    true // 다른 경로는 존재한다고 가정
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}