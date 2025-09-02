package com.bookbook.domain.suspend.controller

import com.bookbook.domain.suspend.dto.request.UserSuspendRequestDto
import com.bookbook.domain.suspend.dto.response.UserSuspendResponseDto
import com.bookbook.domain.suspend.service.SuspendedUserService
import com.bookbook.domain.user.dto.response.UserDetailResponseDto
import com.bookbook.global.jpa.dto.response.PageResponseDto
import com.bookbook.global.rsdata.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// 25.08.28 김지훈

/**
 * 회원 정지 관련 컨트롤러
 *
 * 회원 정지 및 해제 / 정지 이력 페이징 조회
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "SuspendedUserController", description = "어드민 전용 유저 정지 관리 컨트롤러")
class SuspendedUserController (
    private val suspendedUserService: SuspendedUserService
){
    /**
     * 유저 정지 히스토리를 가져옵니다.
     *
     *
     * 페이지 번호와 사이즈의 조합으로 필터링된 페이지를 가져올 수 있습니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 당 항목 수
     * @return 생성된 유저들의 정지 이력 페이지
     */
    @GetMapping("/suspend")
    @Operation(summary = "유저 정지 이력 조회")
    fun getAllSuspendedHistory(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<RsData<PageResponseDto<UserSuspendResponseDto>>> {
        val page = if (page < 1) 1 else page
        val size = if (size < 1) 10 else size

        val histories = suspendedUserService.getSuspendedHistoryPage(page, size, userId)

        val response =  PageResponseDto(histories)

        return ResponseEntity.ok(RsData(
            "200-1",
            "${histories.totalElements}개의 정지 이력을 발견했습니다.",
            response
        ))
    }

    /**
     * 유저를 정지시킵니다.
     *
     * @param requestDto 정지 요청 정보 (유저 ID, 정지 기간, 사유)
     * @return 정지된 유저의 최신 정보
     */
    @PatchMapping("/suspend")
    @Operation(summary = "유저 정지")
    fun suspendUser(
        @Valid @RequestBody requestDto: UserSuspendRequestDto
    ): ResponseEntity<RsData<UserDetailResponseDto>> {
        val suspendedUser = suspendedUserService.addUserAsSuspended(requestDto)

        val userSuspendResponseDto = UserDetailResponseDto(suspendedUser.user)

        return ResponseEntity.ok(
            RsData(
                "200-1",
                "${suspendedUser.user.username}님을 정지하였습니다.",
                userSuspendResponseDto
            )
        )
    }

    /**
     * 유저의 정지를 해제합니다.
     *
     * @param userId 정지를 해제할 유저 ID
     * @return 정지가 해제된 유저의 최신 정보
     */
    @PatchMapping("/{userId}/resume")
    @Operation(summary = "유저 정지 해제")
    fun resumeUser(
        @PathVariable userId: Long
    ): ResponseEntity<RsData<UserDetailResponseDto>> {
        val user = suspendedUserService.resumeUser(userId)
        val userDetailResponseDto = UserDetailResponseDto(user)

        return ResponseEntity.ok(
            RsData(
                "200-1",
                "${userDetailResponseDto.username}님의 정지가 해제되었습니다.",
                userDetailResponseDto
            )
        )
    }
}
