package com.bookbook.domain.report.service

import com.bookbook.domain.report.repository.ReportRepository
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.exception.ServiceException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ReportService 통합 테스트")
@Transactional
class ReportServiceTest {

    @Autowired
    private lateinit var reportService: ReportService

    @Autowired
    private lateinit var reportRepository: ReportRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var reporter: User
    private lateinit var targetUser: User

    @BeforeEach
    fun setUp() {

        reporter = userRepository.save(User(
            username = "reporterUser",
            password = "password",
            nickname = "reporter",
            email = "reporter@test.com",
            address = "서울시",
            registrationCompleted = true
        ))

        targetUser = userRepository.save(User(
            username = "targetUser",
            password = "password",
            nickname = "target",
            email = "target@test.com",
            address = "서울시",
            registrationCompleted = true
        ))
    }

    @Test
    @Transactional
    @DisplayName("신고 생성 성공 테스트")
    fun createReport_success_test() {
        val reason = "불쾌한 콘텐츠 게시"
        reportService.createReport(reporter.id, targetUser.id, reason)
        val createdReport = reportRepository.findByReporterIdAndTargetUserId(reporter.id, targetUser.id).orElse(null)

        assertEquals(reporter.id, createdReport?.reporterUserId)
        assertEquals(targetUser.id, createdReport?.targetUserId)
        assertEquals(reason, createdReport?.reason)
    }

    @Test
    @Transactional
    @DisplayName("신고하는 유저가 존재하지 않을 때 실패 테스트")
    fun createReport_reporterNotFound_test() {
        val nonExistentReporterId = 99L
        val reason = "불쾌한 콘텐츠 게시"
        val exception = assertThrows(ServiceException::class.java) {
            reportService.createReport(nonExistentReporterId, targetUser.id, reason)
        }

        assertEquals("신고한 사용자를 찾을 수 없습니다.", exception.rsData.msg)
    }

    @Test
    @Transactional
    @DisplayName("신고 대상 유저가 존재하지 않을 때 실패 테스트")
    fun createReport_targetUserNotFound_test() {
        val nonExistentTargetId = 99L
        val reason = "불쾌한 콘텐츠 게시"
        val exception = assertThrows(ServiceException::class.java) {
            reportService.createReport(reporter.id, nonExistentTargetId, reason)
        }

        assertEquals("신고 대상 사용자를 찾을 수 없습니다.", exception.rsData.msg)
    }
}