package com.bookbook.global.security

import com.bookbook.TestSetup
import com.bookbook.domain.suspend.repository.SuspendedUserRepository
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.security.LoginSuccessHandler
import com.bookbook.global.security.jwt.JwtProvider
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder

@DisplayName("LoginSuccessHandler 테스트")
@ExtendWith(MockitoExtension::class)
class LoginSuccessHandlerTest {

    private lateinit var loginSuccessHandler: LoginSuccessHandler

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    @Mock
    private lateinit var authentication: Authentication

    @Captor
    private lateinit var cookieCaptor: ArgumentCaptor<Cookie>

    @Mock
    private lateinit var userRepository: UserRepository
    @Mock
    private lateinit var suspendedUserRepository: SuspendedUserRepository
    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testSetup: TestSetup

    private val jwtAccessTokenCookieName = "access-token-test"
    private val jwtRefreshTokenCookieName = "refresh-token-test"
    private val frontendBaseUrl = "http://localhost:3000"
    private val signupPath = "/bookbook/user/signup"
    private val mainPath = "/bookbook"

    @BeforeEach
    fun setUp() {
        testSetup = TestSetup(userRepository, suspendedUserRepository, passwordEncoder)

        loginSuccessHandler = LoginSuccessHandler(
            jwtProvider,
            jwtAccessTokenCookieName,
            jwtRefreshTokenCookieName,
            frontendBaseUrl,
            signupPath,
            mainPath
        )

        mockJwtCookieCreation()
    }

    private fun mockJwtCookieCreation() {
        `when`(jwtProvider.createJwtCookie(anyString(), anyString(), anyInt())).thenAnswer { invocation ->
            val name = invocation.getArgument<String>(0)
            val value = invocation.getArgument<String>(1)
            val maxAge = invocation.getArgument<Int>(2)
            Cookie(name, value).apply { this.maxAge = maxAge }
        }
    }

    private fun mockJwtTokenGeneration(accessTokenValue: String, refreshTokenValue: String) {
        `when`(jwtProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn(accessTokenValue)
        `when`(jwtProvider.generateRefreshToken(anyLong())).thenReturn(refreshTokenValue)
    }

    private fun verifyCookiesAndRedirect(expectedAccessToken: String, expectedRefreshToken: String, expectedRedirectUrl: String) {
        verify(response, times(2)).addCookie(cookieCaptor.capture())
        val addedCookies = cookieCaptor.allValues
        val accessTokenCookie = addedCookies.find { it.name == jwtAccessTokenCookieName }
        val refreshTokenCookie = addedCookies.find { it.name == jwtRefreshTokenCookieName }

        assertEquals(expectedAccessToken, accessTokenCookie?.value)
        assertEquals(expectedRefreshToken, refreshTokenCookie?.value)
        verify(response).sendRedirect(expectedRedirectUrl)
    }

    @Test
    @DisplayName("신규 유저: 회원가입 페이지로 리디렉션")
    fun redirectsToSignupForNewUser() {
        val user = User(
            username = "new_user",
            password = "password",
            email = "newuser@test.com",
            registrationCompleted = false
        )
        val customOAuth2User = testSetup.createCustomOAuth2User(user)
        `when`(authentication.principal).thenReturn(customOAuth2User)

        mockJwtTokenGeneration("access-token-new", "refresh-token-new")
        loginSuccessHandler.onAuthenticationSuccess(request, response, authentication)

        verifyCookiesAndRedirect(
            "access-token-new",
            "refresh-token-new",
            "http://localhost:3000/bookbook/user/signup"
        )
    }

    @Test
    @DisplayName("기존 유저: 메인 페이지로 리디렉션")
    fun redirectsToMainForCompletedUser() {
        val user = User(
            username = "completed_user",
            password = "password",
            email = "completeduser@test.com",
            registrationCompleted = true
        )
        val customOAuth2User = testSetup.createCustomOAuth2User(user)
        `when`(authentication.principal).thenReturn(customOAuth2User)

        mockJwtTokenGeneration("access-token-completed", "refresh-token-completed")
        loginSuccessHandler.onAuthenticationSuccess(request, response, authentication)

        verifyCookiesAndRedirect(
            "access-token-completed",
            "refresh-token-completed",
            "http://localhost:3000/bookbook?login_success=true"
        )
    }
}