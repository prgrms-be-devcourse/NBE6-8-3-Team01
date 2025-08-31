package com.bookbook.global.security

import com.bookbook.global.security.jwt.JwtAuthenticationFilter
import com.bookbook.global.security.jwt.JwtProvider
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val jwtProvider: JwtProvider,
    private val loginSuccessHandler: LoginSuccessHandler,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Value("\${jwt.cookie.name}")
    private lateinit var jwtAccessTokenCookieName: String

    @Value("\${jwt.cookie.refresh-name}")
    private lateinit var jwtRefreshTokenCookieName: String

    @Value("\${frontend.base-url}")
    private lateinit var frontendBaseUrl: String

    @Value("\${frontend.main-path}")
    private lateinit var mainPath: String

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                }
            }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(
                        "/api/*/admin/login",
                        "api/v1/bookbook/users/social/callback",
                        "/login/**",
                        "/bookbook/home",
                        "/api/v1/bookbook/login/oauth2/code/**",
                        "/api/v1/bookbook/home",
                        "/api/v1/bookbook/users/check-nickname",
                        "/api/v1/bookbook/users/signup",
                        "/api/v1/bookbook/users/isAuthenticated",
                        "/api/v1/bookbook/users/logout",
                        "/api/v1/bookbook/auth/refresh-token",
                        "/api/v1/bookbook/home/**",
                        "/favicon.ico",
                        "/h2-console/**",
                        "/images/**",
                        "/uploads/**",
                        "/bookbook/rent/create",
                        "/bookbook/rent/**",
                        "/api/v1/bookbook/rent/**",
                        "/api/v1/bookbook/upload-image",
                        "/api/v1/bookbook/searchbook",
                        "/api/v1/public/**",
                        "/ws/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                    ).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/bookbook/users/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/user/**").permitAll()
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/user/*/rentlist/*/decision").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/v1/bookbook/users/me").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/bookbook/users/me").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/bookbook/users/me").authenticated()
                    .requestMatchers("/api/*/admin/logout", "/api/*/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { userInfo -> userInfo.userService(customOAuth2UserService) }
                    .successHandler(loginSuccessHandler)
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .logout { logout ->
                logout
                    .logoutRequestMatcher { request ->
                        val uri = request.requestURI
                        val method = request.method
                        "/api/v1/bookbook/users/logout" == uri && "GET" == method
                    }
                    .logoutSuccessHandler { request, response, authentication ->
                        // 액세스 토큰 쿠키 삭제
                        val deleteAccessTokenCookie = Cookie(jwtAccessTokenCookieName, null).apply {
                            isHttpOnly = true
                            secure = false
                            path = "/"
                            maxAge = 0
                        }
                        response.addCookie(deleteAccessTokenCookie)

                        // 리프레시 토큰 쿠키 삭제
                        val deleteRefreshTokenCookie = Cookie(jwtRefreshTokenCookieName, null).apply {
                            isHttpOnly = true
                            secure = false
                            path = "/"
                            maxAge = 0
                        }
                        response.addCookie(deleteRefreshTokenCookie)

                        // DB에서 리프레시 토큰 무효화
                        (authentication?.principal as? CustomOAuth2User)?.let { user ->
                            jwtProvider.deleteRefreshToken(user.userId)
                        }

                        response.sendRedirect(frontendBaseUrl + mainPath)
                    }
                    .invalidateHttpSession(false)
                    .deleteCookies(jwtAccessTokenCookieName, jwtRefreshTokenCookieName)
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .headers { headers -> headers.frameOptions { it.sameOrigin() } }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:3000", "https://cdpn.io", "https://nbe-6-8-2-team01.vercel.app")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowCredentials = true
            allowedHeaders = listOf("*")
            maxAge = 3600L
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/bookbook/**", configuration)
            registerCorsConfiguration("/api/**", configuration)
        }
    }
}