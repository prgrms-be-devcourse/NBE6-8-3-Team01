package com.bookbook.global.security.jwt

import com.bookbook.global.exception.ServiceException
import com.bookbook.global.security.CustomOAuth2User
import io.jsonwebtoken.Jwts
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.context.SecurityContextHolder

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {

    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    @Mock
    private lateinit var filterChain: FilterChain

    private val jwtCookieName = "accessToken"

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        jwtAuthenticationFilter = JwtAuthenticationFilter(jwtProvider, jwtCookieName)
    }

    @Test
    @DisplayName("유효한 JWT 토큰이 쿠키에 있는 경우 SecurityContext에 Authentication 객체를 설정한다")
    fun `should set authentication in security context for valid jwt`() {
        // Given
        val jwt = "valid.jwt.token"
        val userId = 1L
        val username = "testuser"
        val role = "USER"
        val claims = Jwts.claims().apply {
            this["userId"] = userId.toInt()
            this["username"] = username
            this["role"] = role
        }
        val cookie = Cookie(jwtCookieName, jwt)

        `when`(request.cookies).thenReturn(arrayOf(cookie))
        `when`(jwtProvider.getAllClaimsFromToken(jwt)).thenReturn(claims)

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // Then
        assertNotNull(SecurityContextHolder.getContext().authentication)
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as CustomOAuth2User
        assertEquals(username, principal.username)
        assertEquals(userId, principal.userId)
        assertTrue(authentication.authorities.any { it.authority == "ROLE_USER" })

        verify(filterChain, times(1)).doFilter(request, response)
    }

    @Test
    @DisplayName("JWT 토큰이 쿠키에 없는 경우 SecurityContext에 변경이 없고 필터 체인이 계속된다")
    fun `should not set authentication if jwt cookie is missing`() {
        // Given
        `when`(request.cookies).thenReturn(null)

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // Then
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain, times(1)).doFilter(request, response)
    }

    @Test
    @DisplayName("JWT 토큰이 유효하지 않은 경우 ServiceException을 발생시키고 401 응답을 반환한다")
    fun `should handle ServiceException for invalid jwt`() {
        // Given
        val jwt = "invalid.jwt.token"
        val cookie = Cookie(jwtCookieName, jwt)
        val serviceException = ServiceException("401-JWT-INVALID", "잘못된 JWT 서명입니다.")
        val errorMessage = serviceException.message

        `when`(request.cookies).thenReturn(arrayOf(cookie))
        `when`(jwtProvider.getAllClaimsFromToken(jwt)).thenThrow(serviceException)

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // Then
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(response, times(1)).sendError(HttpServletResponse.SC_UNAUTHORIZED, errorMessage)

        verify(filterChain, never()).doFilter(request, response)
    }

    @Test
    @DisplayName("JWT 처리 중 예상치 못한 오류가 발생하면 401 응답을 반환한다")
    fun `should handle generic exception`() {
        // Given
        val jwt = "malformed.jwt"
        val cookie = Cookie(jwtCookieName, jwt)

        `when`(request.cookies).thenReturn(arrayOf(cookie))
        `when`(jwtProvider.getAllClaimsFromToken(jwt)).thenThrow(RuntimeException("Unexpected error"))

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        // Then
        assertNull(SecurityContextHolder.getContext().authentication)
        // 일반적인 Exception이 발생하면 JwtAuthenticationFilter는 미리 정의된 메시지를 반환합니다.
        verify(response, times(1)).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), eq("An unexpected error occurred."))

        verify(filterChain, never()).doFilter(request, response)
    }
}