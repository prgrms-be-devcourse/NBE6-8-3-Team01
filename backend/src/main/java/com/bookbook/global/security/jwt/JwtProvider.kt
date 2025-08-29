package com.bookbook.global.security.jwt

import com.bookbook.global.exception.ServiceException
import com.bookbook.global.security.refreshToken.RefreshToken
import com.bookbook.global.security.refreshToken.RefreshTokenRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SecurityException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.Key
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@Component
class JwtProvider(
    @Value("\${jwt.secret-key}")
    private val secretKey: String,
    @param:Value("\${jwt.access-token-validity-in-seconds}")
    private val _accessTokenValidityInSeconds: Int,
    @param:Value("\${jwt.refresh-token-validity-in-seconds}")
    private val _refreshTokenValidityInSeconds: Int,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    private val key: Key by lazy {
        val keyBytes = Decoders.BASE64.decode(secretKey)
        Keys.hmacShaKeyFor(keyBytes)
    }

    val accessTokenValidityInSeconds: Int
        get() = _accessTokenValidityInSeconds

    val refreshTokenValidityInSeconds: Int
        get() = _refreshTokenValidityInSeconds

    companion object {
        private val log = LoggerFactory.getLogger(JwtProvider::class.java)
    }

    // Access Token 생성 메서드
    fun generateAccessToken(userId: Long, username: String, role: String): String {
        val claims: MutableMap<String, Any> = HashMap()
        claims["userId"] = userId
        claims["username"] = username
        claims["role"] = role

        val now = Date()
        val validity = Date(now.time + accessTokenValidityInSeconds * 1000L)

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userId.toString())
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    // Refresh Token 생성 및 저장 메서드
    @Transactional
    fun generateRefreshToken(userId: Long): String {
        val claims: MutableMap<String, Any> = HashMap()
        claims["userId"] = userId

        val now = Date()
        val validity = Date(now.time + refreshTokenValidityInSeconds * 1000L)

        val refreshTokenValue = Jwts.builder()
            .setClaims(claims)
            .setSubject(userId.toString())
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()

        // DB에 기존 리프레시 토큰이 있는지 확인하고 업데이트하거나 새로 저장
        val existingToken = refreshTokenRepository.findByUserId(userId)
        if (existingToken != null) {
            existingToken.updateToken(refreshTokenValue, LocalDateTime.ofInstant(validity.toInstant(), ZoneId.systemDefault()))
        } else {
            refreshTokenRepository.save(
                RefreshToken(
                    token = refreshTokenValue,
                    userId = userId,
                    expiryDate = LocalDateTime.ofInstant(validity.toInstant(), ZoneId.systemDefault())
                )
            )
        }
        return refreshTokenValue
    }

    // 토큰에서 모든 클레임(claims) 추출 및 유효성 검사를 통합
    fun getAllClaimsFromToken(token: String): Claims {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: SecurityException) {
            log.info("잘못된 JWT 서명입니다.", e)
            throw ServiceException("401-JWT-INVALID", "잘못된 JWT 서명입니다.")
        } catch (e: MalformedJwtException) {
            log.info("잘못된 JWT 서명입니다.", e)
            throw ServiceException("401-JWT-INVALID", "잘못된 JWT 서명입니다.")
        } catch (e: ExpiredJwtException) {
            log.info("만료된 JWT 토큰입니다.", e)
            throw ServiceException("401-JWT-EXPIRED", "만료된 JWT 토큰입니다.")
        } catch (e: UnsupportedJwtException) {
            log.info("지원되지 않는 JWT 토큰입니다.", e)
            throw ServiceException("401-JWT-UNSUPPORTED", "지원되지 않는 JWT 토큰입니다.")
        } catch (e: IllegalArgumentException) {
            log.info("JWT 토큰이 잘못되었습니다.", e)
            throw ServiceException("401-JWT-ILLEGAL", "JWT 토큰이 잘못되었습니다.")
        }
    }

    // Refresh Token의 유효성 검사
    @Transactional
    fun validateRefreshToken(refreshTokenValue: String) {
        try {
            // JWT 자체의 유효성 (서명, 구조 등) 검사 - getAllClaimsFromToken이 대신 처리
            val claims = getAllClaimsFromToken(refreshTokenValue)
            val userId = (claims["userId"] as Int).toLong()

            // DB에 저장된 리프레시 토큰과 일치하는지, 만료되지 않았는지 확인
            val storedRefreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
            if (storedRefreshToken == null) {
                throw ServiceException("401-REFRESH-TOKEN-NOT-FOUND", "유효하지 않은 리프레시 토큰입니다.")
            }

            if (storedRefreshToken.userId != userId) {
                throw ServiceException("401-REFRESH-TOKEN-MISMATCH", "토큰 소유자가 일치하지 않습니다.")
            }

            if (storedRefreshToken.isExpired()) {
                refreshTokenRepository.delete(storedRefreshToken)
                throw ServiceException("401-REFRESH-TOKEN-EXPIRED", "만료된 리프레시 토큰입니다. 다시 로그인 해주세요.")
            }

        } catch (e: ServiceException) {
            throw e
        } catch (e: Exception) {
            log.error("리프레시 토큰 유효성 검사 실패: {}", e.message)
            throw ServiceException("401-REFRESH-TOKEN-INVALID", "유효하지 않은 리프레시 토큰입니다.")
        }
    }

    // 리프레시 토큰을 DB에서 삭제하는 메서드
    @Transactional
    fun deleteRefreshToken(userId: Long) {
        refreshTokenRepository.deleteByUserId(userId)
    }
}