package com.bookbook.domain.suspend.schedule

import com.bookbook.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// 25.08.28 김지훈

/**
 * 정지 관련 스케쥴러
 */
@Component
@Profile("!test") // 테스트 환경을 제외하고 작동
class SuspendedUserScheduler(
    private val userRepository: UserRepository
) {

    private val log = LoggerFactory.getLogger(this::class.java)
    /**
     * 정지 자동 해제 스케쥴러
     *
     * 일정 주기마다 활동 재개 가능한 유저를 모두 조회하고,
     * 정지를 자동으로 해제합니다.
     *
     * [[25.09.01]] 15분 마다 확인
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    fun executeScheduledResumingUsers() {
        log.info("자동 정지 해제 스케쥴러 시작")

        val suspendedUsers = userRepository.findAllPossibleResumedUsers()

        if (suspendedUsers.isEmpty()) {
            log.info("정지 해제 요건을 만족한 유저 없음")
            return
        }

        var count = 0
        for (suspendedUser in suspendedUsers) {
            try {
                suspendedUser.resume()
                count++
            } catch (e: RuntimeException) {
                log.warn("멤버: {} 정지 해제 실패", suspendedUser.id)
            }
        }

        log.info("{}명 중 {}명 정지 해제 완료", suspendedUsers.size, count)
    }
}
