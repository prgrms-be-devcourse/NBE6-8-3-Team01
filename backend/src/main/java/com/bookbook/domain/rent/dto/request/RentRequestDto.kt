package com.bookbook.domain.rent.dto.request

import com.bookbook.domain.rent.entity.RentStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

// 25.08.28 현준
// 대여글 등록 시 필요한 정보를 담는 DTO(Data Transfer Object)
data class RentRequestDto(
    @field:NotBlank(message = "대여글 제목을 입력해주세요.")
    val title: String,

    @field:NotBlank(message = "책 상태를 입력해주세요.")
    val bookCondition: String,

    @field:NotBlank(message = "책 이미지 URL을 입력해주세요.")
    val bookImage: String,

    @field:NotBlank(message = "주소를 입력해주세요.")
    val address: String,

    @field:Size(max = 500, message = "내용은 500자를 초과할 수 없습니다.")
    @field:NotBlank(message = "내용을 입력해주세요.")
    val contents: String,

    @field:NotNull(message = "대여 상태를 입력해주세요.")
    val rentStatus: RentStatus, // Enum으로 관리(AVAILABLE, LOANED, FINISHED, DELETED)

    @field:NotBlank(message = "책 제목을 입력해주세요.")
    val bookTitle: String,

    @field:NotBlank(message = "책 저자를 입력해주세요.")
    val author: String,

    @field:NotBlank(message = "책 출판사를 입력해주세요.")
    val publisher: String,

    @field:NotBlank(message = "카테고리를 입력해주세요.")
    val category: String,

    @field:Size(max = 500, message = "내용은 500자를 초과할 수 없습니다.")
    @field:NotBlank(message = "책 설명을 입력해주세요.")
    val description: String
)