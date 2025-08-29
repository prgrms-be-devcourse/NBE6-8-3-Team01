package com.bookbook.domain.rent.controller

import com.bookbook.domain.rent.dto.RentRequestDto
import com.bookbook.domain.rent.dto.RentResponseDto
import com.bookbook.domain.rent.service.RentService
import com.bookbook.global.security.CustomOAuth2User
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

// 25.08.29 현준
// Kotlin으로 변환된 RentController - RESTful 웹 서비스 컨트롤러
@RestController
@RequestMapping("/bookbook/rent", "/api/v1/bookbook/rent") // 기존 경로와 API 경로 모두 지원
@CrossOrigin(origins = ["http://localhost:3000"]) // 프론트엔드 CORS 허용
class RentControllerKt(
    private val rentService: RentService
) {

    // Rent 페이지 등록 Post 요청
    @PostMapping("/create")
    @Operation(summary = "Rent 페이지 등록") // Swagger 에서 API 문서화에 사용되는 설명
    fun createRentPage(
        @RequestBody @Valid dto: RentRequestDto,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User
    ) {
        // 실제 로그인한 사용자 ID 사용
        val userId = customOAuth2User.userId
        rentService.createRentPage(dto, userId)
    }

    // Rent 페이지 조회 Get요청
    @GetMapping("/{id}")
    @Operation(summary = "Rent 페이지 단건 조회")
    fun getRentPage(
        @PathVariable id: Long, // 경로 변수로 전달된 id를 사용, 글 id
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): RentResponseDto {
        // 인증된 사용자 ID 가져오기 (비로그인 사용자는 null)
        val currentUserId = customOAuth2User?.userId
        return rentService.getRentPage(id, currentUserId)
    }

    // Rent 페이지 수정 Put 요청
    @PutMapping("/edit/{id}")
    @Operation(summary = "Rent 페이지 수정")
    fun editRentPage(
        @PathVariable id: Long, // 경로 변수로 전달된 id를 사용, 글 id
        @RequestBody @Valid dto: RentRequestDto,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User
    ) {
        // 실제 로그인한 사용자 ID 사용
        val userId = customOAuth2User.userId
        rentService.editRentPage(id, dto, userId)
    }

    // borrowerUserId : 대여 받은 사용자 ID
    // rentID : 대여 게시글 ID
}