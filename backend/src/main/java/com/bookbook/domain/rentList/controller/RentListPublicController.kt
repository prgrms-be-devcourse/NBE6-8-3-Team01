package com.bookbook.domain.rentList.controller

import com.bookbook.domain.rentList.dto.RentRequestDecisionDto
import com.bookbook.domain.rentList.service.RentListService
import com.bookbook.domain.user.service.UserService
import com.bookbook.global.exception.ServiceException
import com.bookbook.global.rsdata.RsData
import com.bookbook.global.security.jwt.JwtProvider
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

//08-06 유효상

/**
 * 대여 신청 수락/거절을 위한 Public 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/public/rentlist")
@CrossOrigin(origins = ["http://localhost:3000"])
class RentListPublicController(
    private val rentListService: RentListService,
    private val userService: UserService,
    private val jwtProvider: JwtProvider
) {

    companion object {
        private val log = LoggerFactory.getLogger(RentListPublicController::class.java)
    }

    @Value("\${jwt.cookie.name}")
    private lateinit var jwtAccessTokenCookieName: String

    /**
     * 대여 신청 수락/거절 처리 (Public 버전)
     */
    @PatchMapping("/{rentListId}/decision")
    @Operation(summary = "대여 신청 수락/거절 (Public)", description = "JWT 토큰을 직접 검증하여 대여 신청을 처리합니다.")
    fun decideRentRequestPublic(
        @PathVariable rentListId: Long,
        @RequestBody decision: RentRequestDecisionDto,
        request: HttpServletRequest
    ): ResponseEntity<RsData<String?>> {
        log.info(
            "===== Public 대여 신청 수락/거절 API 호출 - RentList ID: {}, 결정: {} =====",
            rentListId, if (decision.approved) "수락" else "거절"
        )
        log.info("===== 디버깅: decision.approved 값: {} =====", decision.approved)
        log.info("===== 디버깅: decision.rejectionReason 값: {} =====", decision.rejectionReason)

        return try {
            // JWT 토큰 추출
            val jwt = extractJwtFromRequest(request)
            if (jwt == null) {
                log.warn("JWT 토큰을 찾을 수 없음")
                return ResponseEntity.status(401)
                    .body(RsData("401-1", "로그인이 필요합니다. JWT 토큰이 없습니다.", "NO_TOKEN"))
            }

            // JWT에서 사용자 정보 추출 (이 과정에서 토큰 유효성도 자동 검증됨)
            val claims = jwtProvider.getAllClaimsFromToken(jwt)
            val userId = claims.get("userId", Long::class.java)

            log.info("JWT에서 추출된 사용자 ID: {}", userId)

            // 사용자 정보 조회
            val currentUser = userService.findById(userId)
            if (currentUser == null) {
                log.error("사용자 ID {}에 해당하는 사용자를 찾을 수 없습니다.", userId)
                return ResponseEntity.status(404)
                    .body(RsData("404-1", "사용자 정보를 찾을 수 없습니다.", null))
            }

            log.info(
                "대여 신청 처리 시작 - 사용자: {}, RentList ID: {}",
                currentUser.nickname, rentListId
            )

            // 대여 신청 처리
            val result = rentListService.decideRentRequest(rentListId, decision, currentUser)
            log.info("대여 신청 처리 완료 - 결과: {}", result)

            ResponseEntity.ok(RsData("200-1", result, "SUCCESS"))

        } catch (e: ServiceException) {
            // JWT 토큰 관련 오류 처리
            if (e.message?.contains("JWT") == true || e.message?.contains("토큰") == true) {
                log.warn("JWT 토큰 오류 - {}", e.message)
                ResponseEntity.status(401)
                    .body(RsData("401-3", e.message ?: "토큰이 유효하지 않습니다.", "TOKEN_ERROR"))
            } else {
                log.error("대여 신청 처리 실패 - RentList ID: {}, 오류: {}", rentListId, e.message, e)
                ResponseEntity.badRequest()
                    .body(RsData("400-1", e.message ?: "알 수 없는 오류", "BUSINESS_ERROR"))
            }
        } catch (e: Exception) {
            log.error("예상치 못한 오류 발생 - RentList ID: {}, 오류: {}", rentListId, e.message, e)
            ResponseEntity.status(500)
                .body(RsData("500-1", "서버 내부 오류가 발생했습니다.", "INTERNAL_ERROR"))
        }
    }

    /**
     * JWT 토큰 추출 메서드
     */
    private fun extractJwtFromRequest(request: HttpServletRequest): String? {
        // Authorization 헤더에서 Bearer 토큰 확인
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7)
        }

        // 쿠키에서 JWT 토큰 확인
        val cookies = request.cookies
        if (cookies != null) {
            for (cookie in cookies) {
                if (jwtAccessTokenCookieName == cookie.name) {
                    return cookie.value
                }
            }
        }

        return null
    }
}