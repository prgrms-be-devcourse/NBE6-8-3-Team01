package com.bookbook.global.security.refreshToken

import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.service.UserService
import com.bookbook.global.exception.ServiceException
import com.bookbook.global.rsdata.RsData
import com.bookbook.global.security.jwt.JwtProvider
import io.jsonwebtoken.Claims
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/api/v1/bookbook/auth")
class RefreshTokenController(
    private val jwtProvider: JwtProvider,
    private val userService: UserService,
    @Value("\${jwt.cookie.name}")
    private val jwtAccessTokenCookieName: String,
    @Value("\${jwt.cookie.refresh-name}")
    private val jwtRefreshTokenCookieName: String
) {
    companion object {
        private val log = LoggerFactory.getLogger(RefreshTokenController::class.java)
    }

    @PostMapping("/refresh-token")
    fun refreshAccessToken(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<RsData<Void?>> {
        val refreshTokenValue: String? = request.cookies?.find {
            it.name == jwtRefreshTokenCookieName
        }?.value

        if (refreshTokenValue.isNullOrBlank()) {
            val rsData: RsData<Void?> = RsData(resultCode = "400-REFRESH-TOKEN-MISSING", msg = "리프레시 토큰이 없습니다.", data = null)
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(rsData)
        }

        return try {
            jwtProvider.validateRefreshToken(refreshTokenValue)

            val claims: Claims = jwtProvider.getAllClaimsFromToken(refreshTokenValue)
            val userId: Long = (claims["userId"] as Int).toLong()

            val user: User = userService.getByIdOrThrow(userId)

            val newAccessToken = jwtProvider.generateAccessToken(user.id, user.username, user.role.name)
            val newRefreshTokenValue = jwtProvider.generateRefreshToken(user.id)

            // JwtProvider의 헬퍼 함수를 사용해 쿠키 생성
            val newAccessTokenCookie = jwtProvider.createJwtCookie(
                jwtAccessTokenCookieName,
                newAccessToken,
                jwtProvider.accessTokenValidityInSeconds
            )
            response.addCookie(newAccessTokenCookie)

            val newRefreshTokenCookie = jwtProvider.createJwtCookie(
                jwtRefreshTokenCookieName,
                newRefreshTokenValue,
                jwtProvider.refreshTokenValidityInSeconds
            )
            response.addCookie(newRefreshTokenCookie)

            val rsData: RsData<Void?> = RsData(resultCode = "200-OK", msg = "새로운 액세스 토큰이 발급되었습니다.", data = null)
            ResponseEntity
                .ok(rsData)
        } catch (e: ServiceException) {
            throw e
        } catch (e: Exception) {
            log.error("토큰 갱신 중 예상치 못한 오류 발생", e)
            val rsData: RsData<Void?> = RsData(resultCode = "500-INTERNAL-SERVER-ERROR", msg = "토큰 갱신 중 오류가 발생했습니다.", data = null)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(rsData)
        }
    }
}