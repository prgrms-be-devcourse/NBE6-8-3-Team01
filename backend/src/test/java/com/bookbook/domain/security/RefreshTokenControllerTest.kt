package com.bookbook.domain.security

import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.enums.Role
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.security.jwt.JwtProvider
import com.bookbook.global.security.refreshToken.RefreshTokenRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.Cookie
import jakarta.transaction.Transactional
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*
import java.security.Key


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("RefreshTokenController 통합 테스트")
@Transactional
class RefreshTokenControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Value("\${jwt.secret-key}")
    private lateinit var secretKey: String

    @Value("\${jwt.cookie.name}")
    private lateinit var jwtAccessTokenCookieName: String

    @Value("\${jwt.cookie.refresh-name}")
    private lateinit var jwtRefreshTokenCookieName: String

    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        testUser = User(username = "testuser", password = "password123", role = Role.USER)
        userRepository.save(testUser)
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 액세스 토큰을 성공적으로 갱신한다")
    fun refreshAccessToken_withValidToken_shouldSucceed() {
        val refreshToken = jwtProvider.generateRefreshToken(testUser.id)

        mockMvc.perform(
            post("/api/v1/bookbook/auth/refresh-token")
                .cookie(Cookie(jwtRefreshTokenCookieName, refreshToken))
        )
            .andExpect(status().isOk)
            .andExpect(cookie().exists(jwtAccessTokenCookieName))
            .andExpect(cookie().exists(jwtRefreshTokenCookieName))
            .andExpect(jsonPath("$.resultCode").value("200-OK"))
            .andExpect(jsonPath("$.msg").value("새로운 액세스 토큰이 발급되었습니다."))
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없는 경우 실패한다")
    fun refreshAccessToken_withMissingToken_shouldFail() {
        mockMvc.perform(
            post("/api/v1/bookbook/auth/refresh-token")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-REFRESH-TOKEN-MISSING"))
            .andExpect(jsonPath("$.msg").value("리프레시 토큰이 없습니다."))
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 갱신 요청 시 실패한다")
    fun refreshAccessToken_withInvalidToken_shouldFail() {
        val invalidRefreshToken = "invalid.token.value"

        mockMvc.perform(
            post("/api/v1/bookbook/auth/refresh-token")
                .cookie(Cookie(jwtRefreshTokenCookieName, invalidRefreshToken))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.resultCode").value("401-JWT-INVALID"))
            .andExpect(jsonPath("$.msg").value("잘못된 JWT 서명입니다."))
    }

    @Test
    @DisplayName("토큰의 사용자 ID로 사용자를 찾을 수 없는 경우 실패한다")
    fun refreshAccessToken_withUserNotFoundInToken_shouldFail() {
        val nonExistentUserId = 999L
        val refreshToken = jwtProvider.generateRefreshToken(nonExistentUserId)

        mockMvc.perform(
            post("/api/v1/bookbook/auth/refresh-token")
                .cookie(Cookie(jwtRefreshTokenCookieName, refreshToken))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-USER-NOT-FOUND"))
            .andExpect(jsonPath("$.msg").value("해당 사용자를 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("리프레시 토큰이 만료된 경우 실패한다")
    fun refreshAccessToken_withExpiredToken_shouldFail() {
        val expiredRefreshToken = generateExpiredRefreshToken(testUser.id)

        mockMvc.perform(
            post("/api/v1/bookbook/auth/refresh-token")
                .cookie(Cookie(jwtRefreshTokenCookieName, expiredRefreshToken))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.resultCode").value("401-JWT-EXPIRED"))
            .andExpect(jsonPath("$.msg").value("만료된 JWT 토큰입니다."))
    }

    private fun generateExpiredRefreshToken(userId: Long): String {
        val key: Key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))
        val now = Date()
        val claims: MutableMap<String, Any> = HashMap()
        claims["userId"] = userId

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userId.toString())
            .setIssuedAt(now)
            .setExpiration(Date(now.time - 1000L))
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }
}