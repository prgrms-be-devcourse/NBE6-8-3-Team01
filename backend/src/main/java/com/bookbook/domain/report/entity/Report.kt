package com.bookbook.domain.report.entity

import com.bookbook.domain.report.enums.ReportStatus
import com.bookbook.domain.user.entity.User
import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import lombok.NoArgsConstructor
import java.time.LocalDateTime

@Entity
@NoArgsConstructor
@AttributeOverride(name = "id", column = Column(name = "report_id"))
class Report(
    reporterUser: User,
    targetUser: User,
    reason: String
) : BaseEntity() {

    var reviewedDate: LocalDateTime? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReportStatus = ReportStatus.PENDING

    @ManyToOne
    @JoinColumn(name = "reporter_user_id", nullable = false)
    var reporterUser: User = reporterUser

    @ManyToOne
    @JoinColumn(name = "target_user_id", nullable = false)
    var targetUser: User = targetUser

    @ManyToOne
    @JoinColumn(name = "closer_id")
    var closer: User? = null

    var reason: String = reason

    fun markAsReviewed() {
        this.status = ReportStatus.REVIEWED
        this.reviewedDate = LocalDateTime.now()
    }

    fun markAsProcessed(closer: User) {
        this.status = ReportStatus.PROCESSED
        this.closer = closer
    }
}