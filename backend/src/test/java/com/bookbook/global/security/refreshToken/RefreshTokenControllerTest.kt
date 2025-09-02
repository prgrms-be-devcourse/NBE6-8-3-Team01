package com.bookbook.global.security.refreshToken

import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.enums.Role
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.security.jwt.JwtProvider
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.Cookie
import jakarta.transaction.Transactional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.security.Key
import java.util.Date
import java.util.HashMap

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
    @DisplayName("유효한 리프레시 토큰으로 토큰 갱신 성공")
    fun refreshSuccess() {
        val refreshToken = jwtProvider.generateRefreshToken(testUser.id)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/bookbook/auth/refresh-token")
                .cookie(Cookie(jwtRefreshTokenCookieName, refreshToken))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.cookie().exists(jwtAccessTokenCookieName))
            .andExpect(MockMvcResultMatchers.cookie().exists(jwtRefreshTokenCookieName))
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-OK"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("새로운 액세스 토큰이 발급되었습니다."))
    }

    @Test
    @DisplayName("리프레시 토큰이 없으면 실패")
    fun refreshFailOnMissingToken() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/bookbook/auth/refresh-token")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-REFRESH-TOKEN-MISSING"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("리프레시 토큰이 없습니다."))
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 갱신 실패")
    fun refreshFailOnInvalidToken() {
        val invalidRefreshToken = "invalid.token.value"

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/bookbook/auth/refresh-token")
                .cookie(Cookie(jwtRefreshTokenCookieName, invalidRefreshToken))
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-JWT-INVALID"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("잘못된 JWT 서명입니다."))
    }

    @Test
    @DisplayName("토큰의 사용자 ID로 사용자를 못 찾으면 실패")
    fun refreshFailOnUserNotFound() {
        val nonExistentUserId = 999L
        val refreshToken = jwtProvider.generateRefreshToken(nonExistentUserId)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/bookbook/auth/refresh-token")
                .cookie(Cookie(jwtRefreshTokenCookieName, refreshToken))
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-USER-NOT-FOUND"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("해당 사용자를 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("만료된 리프레시 토큰으로 갱신 실패")
    fun refreshFailOnExpiredToken() {
        val expiredRefreshToken = generateExpiredRefreshToken(testUser.id)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/bookbook/auth/refresh-token")
                .cookie(Cookie(jwtRefreshTokenCookieName, expiredRefreshToken))
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-JWT-EXPIRED"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("만료된 JWT 토큰입니다."))
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