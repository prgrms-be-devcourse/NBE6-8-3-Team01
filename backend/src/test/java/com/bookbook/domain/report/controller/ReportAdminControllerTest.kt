package com.bookbook.domain.report.controller

import com.bookbook.TestSetup
import com.bookbook.domain.report.enums.ReportStatus
import com.bookbook.domain.report.service.ReportService
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("ReportAdminController 통합 테스트")
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class ReportAdminControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var reportService: ReportService

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var reporterUser: User
    private lateinit var targetUser: User
    private lateinit var adminUser: User

    @BeforeAll
    fun beforeAll() {
        adminUser = userRepository.findById(6).get()
        reporterUser = userRepository.findById(2).get()
        targetUser = userRepository.findById(3).get()

        // 신고가 정상적으로 진행되었다는 가정 하에 진행합니다.

        for (i in 1..12) {
            val (reporter, target) = if (i % 2 == 0) {
                Pair(reporterUser, targetUser)
            } else {
                Pair(targetUser, reporterUser)
            }

            reportService.createReport(
                reporter.id,
                target.id,
                "이유 예시 $i"
            )

            if (i % 3 == 0)
                reportService.getReportDetail(i.toLong())

            if (i == 12)
                reportService.markReportAsProcessed(i.toLong(), adminUser.id)
        }
    }

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        TestSetup.setAuthentication(adminUser)
    }

    @Test
    @DisplayName("신고 목록 조회")
    fun t1() {
        val page = 1
        val size = 10

        val resultActions = mvc.perform(
            get("/api/v1/admin/reports")
            )
            .andDo(print())

        val pageable = PageRequest.of(page - 1, size)

        val reportPage = reportService.getReportPage(pageable, null, null)
        val reports = reportPage.content

        resultActions
            .andExpect(handler().handlerType(ReportAdminController::class.java))
            .andExpect(handler().methodName("getReportPage"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("${reportPage.totalElements}개의 신고글 조회 완료."))
            .andExpect(jsonPath("$.data").exists())

        for (i in reports.indices) {
            val report = reports[i]

            resultActions
                .andExpect(jsonPath("$.data.content[$i].id").value(report.id))
                .andExpect(jsonPath("$.data.content[$i].status").value(report.status.toString()))
                .andExpect(jsonPath("$.data.content[$i].reporterUserId").exists())
                .andExpect(jsonPath("$.data.content[$i].targetUserId").exists())
                .andExpect(jsonPath("$.data.content[$i].createdDate")
                    .value(Matchers.startsWith(report.createdDate.toString().take(20)))
                )
        }
    }

    @Test
    @DisplayName("신고 목록 조회 - 필터링 (상태)")
    fun t2() {
        val page = 1
        val size = 10
        val statuses = listOf(ReportStatus.REVIEWED, ReportStatus.PROCESSED)

        val resultActions = mvc.perform(
            get("/api/v1/admin/reports")
                .apply {
                    statuses.forEach {
                        param("status", it.name)
                    }
                }
            )
            .andDo(print())

        val pageable = PageRequest.of(page - 1, size)

        val reportPage = reportService.getReportPage(pageable, statuses, null)
        val reports = reportPage.content

        resultActions
            .andExpect(handler().handlerType(ReportAdminController::class.java))
            .andExpect(handler().methodName("getReportPage"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data").exists())

        for (i in reports.indices) {
            val report = reports[i]

            resultActions
                .andExpect(jsonPath("$.data.content[$i].id").value(report.id))
                .andExpect(jsonPath("$.data.content[$i].status")
                    .value(Matchers.not(ReportStatus.PENDING.toString()))
                )
                .andExpect(jsonPath("$.data.content[$i].createdDate")
                    .value(Matchers.startsWith(report.createdDate.toString().take(20)))
                )
        }
    }

    @Test
    @DisplayName("신고 목록 조회 - 상태 필터링(신고 대상 ID)")
    fun t3() {
        val page = 1
        val size = 10
        val userId = targetUser.id

        val resultActions = mvc.perform(
            get("/api/v1/admin/reports")
                .param("targetUserId", userId.toString())
            )
            .andDo(print())

        val pageable = PageRequest.of(page - 1, size)

        val reportPage = reportService.getReportPage(pageable, null, userId)
        val reports = reportPage.content

        resultActions
            .andExpect(handler().handlerType(ReportAdminController::class.java))
            .andExpect(handler().methodName("getReportPage"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data").exists())

        for (i in reports.indices) {
            val report = reports[i]

            resultActions
                .andExpect(jsonPath("$.data.content[$i].id").value(report.id))
                .andExpect(jsonPath("$.data.content[$i].targetUserId").value(userId))
                .andExpect(jsonPath("$.data.content[$i].createdDate")
                    .value(Matchers.startsWith(report.createdDate.toString().take(20)))
                )
        }
    }

    @Test
    @DisplayName("단일 신고 조회")
    fun t4() {
        val reportId = 1L

        val resultActions = mvc.perform(
            get("/api/v1/admin/reports/${reportId}/review")
            )
            .andDo(print())

        val reportDetail = reportService.getReportDetail(reportId)

        resultActions
            .andExpect(handler().handlerType(ReportAdminController::class.java))
            .andExpect(handler().methodName("getReportDetail"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("${reportId}번 신고글 조회 완료"))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.id").value(reportId))
            .andExpect(jsonPath("$.data.status").value(ReportStatus.REVIEWED.toString()))
            .andExpect(jsonPath("$.data.reviewedDate")
                .value(Matchers.startsWith(reportDetail.reviewedDate.toString().take(20)))
            )
    }

    @Test
    @DisplayName("단일 신고 처리 - 정상 진행")
    fun t5() {
        // 리뷰가 완료된 신고글의 Id
        val reportId = 3L

        // 처리 완료 진행
        val resultActions = mvc.perform(
            patch("/api/v1/admin/reports/${reportId}/process")
            )
            .andDo(print())

        val report = reportService.findReportById(reportId)

        resultActions
            .andExpect(handler().handlerType(ReportAdminController::class.java))
            .andExpect(handler().methodName("processReport"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("${reportId}번 신고가 정상적으로 처리되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())

        assertThat(report.status).isEqualTo(ReportStatus.PROCESSED)
        assertThat(report.closerId).isEqualTo(adminUser.id)
    }

    @Test
    @DisplayName("단일 신고 처리 - 존재하지 않는 신고 ID")
    fun t6() {
        val reportId = 99L

        // 처리 완료 진행
        val resultActions = mvc.perform(
            patch("/api/v1/admin/reports/${reportId}/process")
            )
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(ReportAdminController::class.java))
            .andExpect(handler().methodName("processReport"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
            .andExpect(jsonPath("$.msg").value("존재하지 않는 신고입니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("단일 신고 처리 - 처리 대기 상태인 신고 처리")
    fun t7() {
        val reportId = 1L

        // 처리 완료 진행
        val resultActions = mvc.perform(
            patch("/api/v1/admin/reports/${reportId}/process")
            )
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(ReportAdminController::class.java))
            .andExpect(handler().methodName("processReport"))
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.resultCode").value("422-1"))
            .andExpect(jsonPath("$.msg").value("해당 신고를 먼저 확인해야 합니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("단일 신고 처리 - 처리를 완료한 신고를 다시 처리")
    fun t8() {
        val reportId = 3L

        // 처리 완료 진행
        mvc.perform(
            patch("/api/v1/admin/reports/${reportId}/process")
        ).andDo(print())

        val resultActions = mvc.perform(
            patch("/api/v1/admin/reports/${reportId}/process")
            )
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(ReportAdminController::class.java))
            .andExpect(handler().methodName("processReport"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.resultCode").value("409-1"))
            .andExpect(jsonPath("$.msg").value("해당 신고는 이미 처리가 완료되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    @DisplayName("단일 신고 처리 - 일반 유저가 진행")
    fun t9() {
        val reportId = 3L

        TestSetup.setAuthentication(targetUser)

        // 처리 완료 진행
        val resultActions = mvc.perform(
            patch("/api/v1/admin/reports/${reportId}/process")
            )
            .andDo(print())

        resultActions
            .andExpect(handler().handlerType(ReportAdminController::class.java))
            .andExpect(handler().methodName("processReport"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.resultCode").value("401-1"))
            .andExpect(jsonPath("$.msg").value("허가되지 않은 접근입니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }
}