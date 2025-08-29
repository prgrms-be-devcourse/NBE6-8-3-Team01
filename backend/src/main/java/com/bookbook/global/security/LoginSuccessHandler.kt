package com.bookbook.global.security

import com.bookbook.global.security.jwt.JwtProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class LoginSuccessHandler(
    private val jwtProvider: JwtProvider,
    @Value("\${jwt.cookie.name}")
    private val jwtAccessTokenCookieName: String,
    @Value("\${jwt.cookie.refresh-name}")
    private val jwtRefreshTokenCookieName: String,
    @Value("\${frontend.base-url}")
    private val frontendBaseUrl: String,
    @Value("\${frontend.signup-path}")
    private val signupPath: String,
    @Value("\${frontend.main-path}")
    private val mainPath: String
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauth2User = authentication.principal as CustomOAuth2User

        println("DEBUG: Login success for username: ${oauth2User.username}, isRegistrationCompleted: ${oauth2User.isRegistrationCompleted}")

        val accessToken = jwtProvider.generateAccessToken(
            oauth2User.userId,
            oauth2User.username,
            oauth2User.role.name
        )

        val refreshToken = jwtProvider.generateRefreshToken(oauth2User.userId)

        // JwtProvider의 헬퍼 함수를 사용해 쿠키 생성
        val jwtAccessTokenCookie = jwtProvider.createJwtCookie(
            jwtAccessTokenCookieName,
            accessToken,
            jwtProvider.accessTokenValidityInSeconds
        )
        response.addCookie(jwtAccessTokenCookie)

        val jwtRefreshTokenCookie = jwtProvider.createJwtCookie(
            jwtRefreshTokenCookieName,
            refreshToken,
            jwtProvider.refreshTokenValidityInSeconds
        )
        response.addCookie(jwtRefreshTokenCookie)

        val redirectUrl = if (!oauth2User.isRegistrationCompleted) {
            "${frontendBaseUrl}${signupPath}".also {
                println("Redirecting to signup page: $it")
            }
        } else {
            val queryParam = "?login_success=true"
            "${frontendBaseUrl}${mainPath}${queryParam}".also {
                println("Redirecting to main page: $it")
            }
        }

        response.sendRedirect(redirectUrl)
    }
}