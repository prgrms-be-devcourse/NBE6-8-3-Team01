package com.bookbook.global.security.jwt

import com.bookbook.global.exception.ServiceException
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

@DisplayName("JwtAuthenticationFilter 테스트")
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
    @DisplayName("유효한 JWT 토큰이 있으면 인증 객체를 설정")
    fun setAuthenticationWithValidJwt() {
        val jwt = "valid.jwt.token"
        val cookie = Cookie(jwtCookieName, jwt)
        `when`(request.cookies).thenReturn(arrayOf(cookie))
        `when`(jwtProvider.getAllClaimsFromToken(anyString())).thenReturn(Jwts.claims().apply {
            this["userId"] = 1
            this["username"] = "testuser"
            this["role"] = "USER"
        })

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        assertNotNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain, times(1)).doFilter(request, response)
    }

    @Test
    @DisplayName("JWT 토큰이 없으면 변경 없이 필터 체인 계속")
    fun continueFilterChainWithoutJwt() {
        `when`(request.cookies).thenReturn(null)

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain, times(1)).doFilter(request, response)
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰은 401 응답")
    fun handleInvalidJwt() {
        val jwt = "invalid.jwt.token"
        val cookie = Cookie(jwtCookieName, jwt)
        `when`(request.cookies).thenReturn(arrayOf(cookie))
        `when`(jwtProvider.getAllClaimsFromToken(anyString())).thenThrow(ServiceException("401-JWT-INVALID", "잘못된 JWT 서명입니다."))

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(response, times(1)).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString())
        verify(filterChain, never()).doFilter(request, response)
    }

    @Test
    @DisplayName("예상치 못한 오류 발생 시 401 응답")
    fun handleGenericException() {
        val jwt = "malformed.jwt"
        val cookie = Cookie(jwtCookieName, jwt)
        `when`(request.cookies).thenReturn(arrayOf(cookie))
        `when`(jwtProvider.getAllClaimsFromToken(anyString())).thenThrow(RuntimeException("Unexpected error"))

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify(response, times(1)).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString())
        verify(filterChain, never()).doFilter(request, response)
    }
}