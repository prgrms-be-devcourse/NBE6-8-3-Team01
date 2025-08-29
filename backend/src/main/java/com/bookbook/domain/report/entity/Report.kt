package com.bookbook.domain.report.entity

import com.bookbook.domain.report.enums.ReportStatus
import com.bookbook.domain.user.entity.User
import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@AttributeOverride(name = "id", column = Column(name = "report_id"))
class Report(
    @Column(name = "reporter_user_id", nullable = false)
    var reporterUserId: Long,

    @Column(name = "target_user_id", nullable = false)
    var targetUserId: Long,

    var reason: String
) : BaseEntity() {

    lateinit var reviewedDate: LocalDateTime

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReportStatus = ReportStatus.PENDING

    @Column(name = "closer_id")
    var closerId: Long? = null

    fun markAsReviewed() {
        this.status = ReportStatus.REVIEWED
        this.reviewedDate = LocalDateTime.now()
    }

    fun markAsProcessed(closer: User) {
        this.status = ReportStatus.PROCESSED
        this.closerId = closer.id
    }
}