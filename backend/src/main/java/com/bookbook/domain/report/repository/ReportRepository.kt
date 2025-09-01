package com.bookbook.domain.report.repository

import com.bookbook.domain.report.entity.Report
import com.bookbook.domain.report.enums.ReportStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface ReportRepository : JpaRepository<Report, Long> {
    @Query(
        """
        SELECT r FROM Report r WHERE
        (:targetId IS NULL OR r.targetUserId = :targetId) AND
        (:status IS NULL OR r.status IN :status)
        ORDER BY r.createdDate DESC
    """
    )
    fun findFilteredReportHistory(
        pageable: Pageable,
        @Param("status") status: List<ReportStatus>?,
        @Param("targetId") targetId: Long?
    ): Page<Report>

    @Query("SELECT r FROM Report r WHERE r.reporterUserId = :reporterId AND r.targetUserId = :targetUserId")
    fun findByReporterIdAndTargetUserId(
        @Param("reporterId") reporterId: Long,
        @Param("targetUserId") targetUserId: Long
    ): Optional<Report>
}
