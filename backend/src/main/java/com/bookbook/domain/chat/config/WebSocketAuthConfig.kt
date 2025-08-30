package com.bookbook.domain.chat.config

import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.security.CustomOAuth2User
import com.bookbook.global.security.jwt.JwtProvider
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
class WebSocketAuthConfig(
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository
) : WebSocketMessageBrokerConfigurer {
    
    companion object {
        private val log = LoggerFactory.getLogger(WebSocketAuthConfig::class.java)
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
                val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

                if (accessor != null && StompCommand.CONNECT == accessor.command) {
                    // 쿠키에서 JWT 토큰 추출
                    val token = extractJwtFromCookie(accessor)

                    if (token != null) {
                        try {
                            // JWT에서 사용자 정보 추출
                            val claims = jwtProvider.getAllClaimsFromToken(token)
                            val userId = claims.get("userId", Long::class.java)
                            val role = claims.get("role", String::class.java)

                            // 사용자 정보 조회
                            val user = userRepository.findById(userId).orElse(null)
                            if (user != null) {
                                // 간단한 attributes 맵 생성
                                val attributes: MutableMap<String, Any> = mutableMapOf(
                                    "id" to user.id,
                                    "username" to user.username,
                                    "nickname" to (user.nickname ?: "")
                                )

                                // CustomOAuth2User 생성
                                val customUser = CustomOAuth2User(
                                    mutableSetOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_$role")),
                                    attributes,
                                    "id",  // nameAttributeKey
                                    user.username,
                                    user.nickname,
                                    user.id,
                                    user.isRegistrationCompleted(),
                                    user.role
                                )

                                // Authentication 객체 생성
                                val authentication: Authentication = UsernamePasswordAuthenticationToken(
                                    customUser,
                                    null,
                                    customUser.authorities
                                )

                                accessor.user = authentication
                                log.info("WebSocket JWT 인증 성공 - userId: {}", userId)
                            }
                        } catch (e: Exception) {
                            log.error("WebSocket JWT 인증 실패", e)
                        }
                    } else {
                        log.warn("WebSocket 연결 시 유효하지 않은 JWT 토큰")
                    }
                }

                return message
            }
        })
    }

    private fun extractJwtFromCookie(accessor: StompHeaderAccessor): String? {
        // STOMP 헤더에서 쿠키 추출
        val cookieHeader = accessor.getFirstNativeHeader("cookie")
        if (cookieHeader != null) {
            val cookies = cookieHeader.split(";")
            for (cookie in cookies) {
                val cookieParts = cookie.trim().split("=")
                if (cookieParts.size == 2 && "JWT_TOKEN" == cookieParts[0]) {
                    return cookieParts[1]
                }
            }
        }
        return null
    }
}