package com.bookbook.domain.rentList.controller

import com.bookbook.domain.rentList.dto.RentListCreateRequestDto
import com.bookbook.domain.rentList.dto.RentListResponseDto
import com.bookbook.domain.rentList.dto.RentRequestDecisionDto
import com.bookbook.domain.rentList.service.RentListService
import com.bookbook.domain.review.dto.ReviewCreateRequestDto
import com.bookbook.domain.review.dto.ReviewResponseDto
import com.bookbook.domain.review.service.ReviewService
import com.bookbook.domain.user.service.UserService
import com.bookbook.global.exception.ServiceException
import com.bookbook.global.rsdata.RsData
import com.bookbook.global.security.CustomOAuth2User
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 내가 빌린 도서 목록 관리 컨트롤러
 *
 * 사용자가 대여한 도서 목록의 조회, 대여 신청 및 대여받은 사람이 대여자에게 리뷰를 작성하는 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/user/{borrowerUserId}/rentlist")
class RentListController(
    private val rentListService: RentListService,
    private val reviewService: ReviewService,
    private val userService: UserService
) {
    /**
     * 내가 빌린 도서 목록 조회 (검색 기능 포함)
     *
     * @param borrowerUserId 대여받은 사용자 ID
     * @param search 검색어 (선택사항, 책 제목/저자/출판사/게시글 제목에서 검색)
     * @return 대여한 도서 목록
     */
    @GetMapping
    fun getRentListByUserId(
        @PathVariable borrowerUserId: Long,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<RsData<List<RentListResponseDto>>> {
        val rentList = if (search.isNullOrBlank()) {
            rentListService.getRentListByUserId(borrowerUserId)
        } else {
            rentListService.searchRentListByUserId(borrowerUserId, search)
        }

        return ResponseEntity.ok(
            RsData("200", "대여 도서 목록을 조회했습니다.", rentList)
        )
    }

    /**
     * 도서 대여 신청 등록
     *
     * 사용자가 원하는 도서에 대해 대여 신청을 등록합니다.
     * 반납일은 대여일로부터 자동으로 14일 후로 설정됩니다.
     *
     * @param borrowerUserId 대여받을 사용자 ID
     * @param request 대여 신청 정보 (대여일, 게시글 ID)
     * @return 생성된 대여 기록 정보
     */
    @PostMapping("/create")
    @Operation(summary = "Rent_List 등록")
    fun createRentList(
        @PathVariable borrowerUserId: Long,
        @RequestBody request: RentListCreateRequestDto
    ): ResponseEntity<RsData<Void?>> {
        return try {
            rentListService.createRentList(borrowerUserId, request)
            ResponseEntity.ok(RsData("200-1", "대여 신청이 완료되었습니다."))
        } catch (e: ServiceException) {
            ResponseEntity.status(e.rsData.statusCode)
                .body(RsData(e.rsData.resultCode, e.rsData.msg))
        } catch (e: Exception) {
            ResponseEntity.internalServerError()
                .body(RsData("500-1", "대여 신청 처리 중 오류가 발생했습니다: ${e.message}"))
        }
    }

    /**
     * 대여 신청 수락/거절 처리
     *
     * 책 소유자가 들어온 대여 신청에 대해 수락 또는 거절을 결정합니다.
     * 수락 시: 책 상태를 '대여 중'으로 변경하고 신청자에게 수락 알림 발송
     * 거절 시: 신청자에게 거절 알림 발송
     *
     * @param rentListId 대여 신청 ID
     * @param decision 수락/거절 결정 정보
     * @param customOAuth2User 현재 로그인한 사용자 (책 소유자)
     * @return 처리 결과
     */
    @PatchMapping("/{rentListId}/decision")
    @Operation(summary = "대여 신청 수락/거절", description = "책 소유자가 대여 신청에 대해 수락 또는 거절을 결정합니다.")
    fun decideRentRequest(
        @PathVariable rentListId: Long,
        @RequestBody decision: RentRequestDecisionDto,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): ResponseEntity<RsData<Void?>> {
        if (customOAuth2User?.userId == null) {
            return ResponseEntity.status(401)
                .body(RsData("401-1", "로그인 후 사용해주세요."))
        }

        val currentUser = userService.findById(customOAuth2User.userId)
            ?: return ResponseEntity.status(404)
                .body(RsData("404-1", "사용자 정보를 찾을 수 없습니다."))

        return try {
            val result = rentListService.decideRentRequest(rentListId, decision, currentUser)
            ResponseEntity.ok(RsData("200-1", result))
        } catch (e: ServiceException) {
            ResponseEntity.status(e.rsData.statusCode)
                .body(RsData(e.rsData.resultCode, e.rsData.msg))
        } catch (e: Exception) {
            ResponseEntity.internalServerError()
                .body(RsData("500-1", "대여 신청 처리 중 오류가 발생했습니다: ${e.message}"))
        }
    }

    /**
     * 도서 반납하기
     *
     * 대여받은 사람이 도서를 조기 반납하는 기능입니다.
     * 반납 시 해당 대여 기록의 상태와 원본 게시글의 상태를 업데이트합니다.
     *
     * @param borrowerUserId 대여받은 사용자 ID
     * @param rentId 대여 게시글 ID
     * @return 성공 메시지
     */
    @PatchMapping("/{rentId}/return")
    fun returnBook(
        @PathVariable borrowerUserId: Long,
        @PathVariable rentId: Long,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): ResponseEntity<RsData<Void?>> {
        if (customOAuth2User?.userId == null) {
            return ResponseEntity.status(401)
                .body(RsData("401-1", "로그인 후 사용해주세요."))
        }

        if (customOAuth2User.userId != borrowerUserId) {
            return ResponseEntity.status(403)
                .body(RsData("403-1", "본인이 대여한 도서만 반납할 수 있습니다."))
        }

        return try {
            rentListService.returnBook(borrowerUserId, rentId)
            ResponseEntity.ok(RsData("200", "도서가 성공적으로 반납되었습니다."))
        } catch (e: ServiceException) {
            ResponseEntity.status(e.rsData.statusCode)
                .body(RsData(e.rsData.resultCode, e.rsData.msg))
        } catch (e: Exception) {
            ResponseEntity.internalServerError()
                .body(RsData("500-1", "반납 처리 중 오류가 발생했습니다: ${e.message}"))
        }
    }

    /**
     * 대여받은 사람이 대여자에게 리뷰 작성
     *
     * 거래 완료 후 도서를 빌린 사람이 빌려준 사람을 평가하는 기능입니다.
     *
     * @param borrowerUserId 대여받은 사람(리뷰 작성자) ID
     * @param rentId 대여 게시글 ID
     * @param request 리뷰 생성 요청 데이터 (평점 등)
     * @return 생성된 리뷰 정보
     */
    @PostMapping("/{rentId}/review")
    fun createBorrowerReview(
        @PathVariable borrowerUserId: Long,
        @PathVariable rentId: Long,
        @RequestBody request: ReviewCreateRequestDto
    ): ResponseEntity<RsData<ReviewResponseDto>> {
        val review = reviewService.createBorrowerReview(borrowerUserId, rentId, request)
        return ResponseEntity.ok(RsData("200", "리뷰를 작성했습니다.", review))
    }
}