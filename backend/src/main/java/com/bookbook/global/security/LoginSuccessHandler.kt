package com.bookbook.global.security

import com.bookbook.global.security.jwt.JwtProvider
import jakarta.servlet.ServletException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException

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

        // 1. JWT Access Token 생성
        val accessToken = jwtProvider.generateAccessToken(
            oauth2User.userId,
            oauth2User.username,
            oauth2User.role.name
        )

        // 2. JWT Refresh Token 생성 및 DB 저장
        val refreshToken = jwtProvider.generateRefreshToken(oauth2User.userId)

        // 3. JWT Access Token을 HTTP Only 쿠키에 담아 전송
        val jwtAccessTokenCookie = Cookie(jwtAccessTokenCookieName, accessToken).apply {
            isHttpOnly = true
            secure = false
            path = "/"
            maxAge = jwtProvider.accessTokenValidityInSeconds
        }
        response.addCookie(jwtAccessTokenCookie)

        // 4. JWT Refresh Token을 HTTP Only 쿠키에 담아 전송
        val jwtRefreshTokenCookie = Cookie(jwtRefreshTokenCookieName, refreshToken).apply {
            isHttpOnly = true
            secure = false
            path = "/"
            maxAge = jwtProvider.refreshTokenValidityInSeconds
        }
        response.addCookie(jwtRefreshTokenCookie)

        // 5. 회원가입 완료 여부에 따라 프론트엔드로 리다이렉트
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