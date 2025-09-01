package com.bookbook.global.security.jwt

import com.bookbook.domain.user.enums.Role
import com.bookbook.global.exception.ServiceException
import com.bookbook.global.security.CustomOAuth2User
import io.jsonwebtoken.Claims
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Suppress("UNCHECKED_CAST")
@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    @Value("\${jwt.cookie.name}")
    private val jwtCookieName: String
) : OncePerRequestFilter() {

    companion object {
        private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    }

    @Throws(ServletException::class, IOException::class)
    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val jwt = getJwtFromCookie(request)

            if (jwt != null) {
                val claims: Claims = jwtProvider.getAllClaimsFromToken(jwt)
                val userId = (claims["userId"] as Int).toLong()
                val username = claims["username"] as String
                val roleString = claims["role"] as String
                val role = Role.valueOf(roleString)

                val authorities = listOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_${role.name}"))
                val attributes = mapOf<String, Any>("username" to username)

                val customOAuth2User = CustomOAuth2User(
                    authorities,
                    attributes,
                    "username",
                    username,
                    null,
                    userId,
                    true,
                    role
                )

                val authentication = UsernamePasswordAuthenticationToken(
                    customOAuth2User, null, authorities
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (ex: ServiceException) {
            log.error("Authentication failed with ServiceException: {}", ex.message)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.message)
            return
        } catch (ex: Exception) {
            log.error("Could not set user authentication in security context", ex)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "An unexpected error occurred.")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun getJwtFromCookie(request: HttpServletRequest): String? {
        val cookies = request.cookies
        if (cookies != null) {
            for (cookie in cookies) {
                if (jwtCookieName == cookie.name) {
                    return cookie.value
                }
            }
        }
        return null
    }
}