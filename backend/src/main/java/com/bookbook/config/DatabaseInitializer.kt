package com.bookbook.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component

//08-31 유효상

/**
 * 데이터베이스 초기화 및 제약조건 업데이트 컴포넌트
 */
@Component
@Profile("!production") // production 환경을 제외한 모든 환경에서 실행
class DatabaseInitializer(
    private val jdbcTemplate: JdbcTemplate
) {
    companion object {
        private val log = LoggerFactory.getLogger(DatabaseInitializer::class.java)
    }

    @PostConstruct
    fun updateNotificationTypeConstraint() {
        try {
            log.info("===== 알림 타입 제약조건 업데이트 시작 =====")

            // 1. 기존 제약조건 삭제 (존재하는 경우)
            try {
                jdbcTemplate.execute("ALTER TABLE notification DROP CONSTRAINT IF EXISTS NOTIFICATION_TYPE_CHECK")
                log.info("기존 NOTIFICATION_TYPE_CHECK 제약조건 삭제 완료")
            } catch (e: Exception) {
                log.warn("기존 제약조건 삭제 시 오류 (무시): ${e.message}")
            }

            // 2. 새로운 제약조건 추가 (모든 NotificationType enum 값 포함)
            val newConstraint = "ALTER TABLE notification ADD CONSTRAINT NOTIFICATION_TYPE_CHECK " +
                    "CHECK (type IN ('RENT_REQUEST', 'RENT_APPROVED', 'RENT_REJECTED', 'WISHLIST_AVAILABLE', 'RETURN_REMINDER', 'POST_CREATED'))"

            jdbcTemplate.execute(newConstraint)
            log.info("새로운 NOTIFICATION_TYPE_CHECK 제약조건 추가 완료")

            // 3. 제약조건 확인
            val constraintCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS WHERE CONSTRAINT_NAME = 'NOTIFICATION_TYPE_CHECK'",
                Int::class.java
            )

            if (constraintCount != null && constraintCount > 0) {
                log.info("✅ NOTIFICATION_TYPE_CHECK 제약조건이 성공적으로 설정되었습니다")
            } else {
                log.warn("⚠️ NOTIFICATION_TYPE_CHECK 제약조건 설정을 확인할 수 없습니다")
            }

            // 4. 현재 notification 테이블의 type 컬럼 값들 확인
            try {
                val notificationCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM notification", 
                    Int::class.java
                )
                log.info("현재 notification 테이블의 레코드 수: $notificationCount")

                if (notificationCount != null && notificationCount > 0) {
                    jdbcTemplate.query(
                        "SELECT DISTINCT type FROM notification",
                        RowMapper { rs, _ ->
                            val type = rs.getString("type")
                            log.info("기존 알림 타입: $type")
                            type
                        }
                    )
                }
            } catch (e: Exception) {
                log.warn("기존 데이터 확인 중 오류: ${e.message}")
            }

            log.info("===== 알림 타입 제약조건 업데이트 완료 =====")
        } catch (e: Exception) {
            log.error("알림 타입 제약조건 업데이트 실패: ${e.message}", e)
            // 애플리케이션 시작을 중단하지 않고 계속 진행
        }

        // isProcessed 컬럼 초기화
        updateNotificationProcessedColumn()
    }

    private fun updateNotificationProcessedColumn() {
        try {
            log.info("===== 알림 isProcessed 컬럼 업데이트 시작 =====")

            // 1. isProcessed 컬럼이 존재하는지 확인
            val columnExists = try {
                jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'NOTIFICATION' AND COLUMN_NAME = 'IS_PROCESSED'",
                    Int::class.java
                ) ?: 0 > 0
            } catch (e: Exception) {
                false
            }

            if (!columnExists) {
                // 2. 컬럼이 없으면 기본값 false로 추가
                jdbcTemplate.execute("ALTER TABLE notification ADD COLUMN is_processed BOOLEAN DEFAULT FALSE")
                log.info("isProcessed 컬럼 추가 완료")
            }

            // 3. 기존 데이터의 isProcessed 값을 false로 설정
            val updatedCount = jdbcTemplate.update("UPDATE notification SET is_processed = FALSE WHERE is_processed IS NULL")
            log.info("기존 알림 데이터 isProcessed 값 설정 완료: $updatedCount 건")

            log.info("===== 알림 isProcessed 컬럼 업데이트 완료 =====")
        } catch (e: Exception) {
            log.error("isProcessed 컬럼 업데이트 실패: ${e.message}", e)
        }
    }
}
