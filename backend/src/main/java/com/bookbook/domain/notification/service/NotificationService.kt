package com.bookbook.domain.notification.service

import com.bookbook.domain.notification.dto.NotificationResponseDto
import com.bookbook.domain.notification.entity.Notification
import com.bookbook.domain.notification.enums.NotificationType
import com.bookbook.domain.notification.repository.NotificationRepository
import com.bookbook.domain.rent.entity.RentStatus
import com.bookbook.domain.rent.repository.RentRepository
import com.bookbook.domain.rentList.entity.RentRequestStatus
import com.bookbook.domain.rentList.repository.RentListRepository
import com.bookbook.domain.user.entity.User
import com.bookbook.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

//08-29 유효상
//
// 1. RentList.kt 변환 완료 - reflection 코드 제거하고 직접 필드 접근으로 변경됨
//  향후 리팩토링 필요 - 다른 도메인들이 Kotlin으로 변환되면 수정 필요한 부분들:
// 2. User.java → User.kt 변환 시: 필드 접근 방식 확인
// 3. Rent.java → Rent.kt 변환 시: 필드 접근 방식 확인
//
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val rentListRepository: RentListRepository,
    private val rentRepository: RentRepository
) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    // 사용자별 알림 조회
    @Transactional(readOnly = true)
    fun getNotificationsByUser(user: User): List<NotificationResponseDto> {
        val notifications = notificationRepository.findByReceiverOrderByCreatedDateDesc(user)

        return notifications.map { notification ->
            NotificationResponseDto.from(notification)
        }
    }

    // 읽지 않은 알림 개수
    @Transactional(readOnly = true)
    fun getUnreadCount(user: User): Long {
        return notificationRepository.countByReceiverAndIsReadFalse(user)
    }

    // 특정 알림 읽음 처리
    @Transactional
    fun markAsRead(notificationId: Long, user: User) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 알림입니다. ID: $notificationId") }

        // 본인의 알림인지 확인
        if (notification.receiver != user) {
            throw ServiceException("403-1", "다른 사용자의 알림에 접근할 수 없습니다.")
        }

        notification.markAsRead()
        notificationRepository.save(notification)
    }

    // 모든 알림 읽음 처리
    @Transactional
    fun markAllAsRead(user: User) {
        notificationRepository.markAllAsReadByReceiver(user)
    }

    // 특정 대여 건의 RENT_REQUEST 알림을 처리 완료로 표시
    @Transactional
    fun markRentRequestAsProcessed(rentId: Long, receiver: User) {
        val notifications = notificationRepository.findByReceiverAndTypeAndRelatedId(
            receiver, NotificationType.RENT_REQUEST, rentId
        )
        
        notifications.forEach { notification ->
            notification.markAsProcessed()
            notificationRepository.save(notification)
        }
        
        log.info("RENT_REQUEST 알림 처리 완료 표시 - 수신자: {}, Rent ID: {}, 처리된 알림 수: {}", 
            receiver.nickname, rentId, notifications.size)
    }

    // 새 알림 생성 (다른 서비스에서 호출용)
    @Transactional
    fun createNotification(
        receiver: User,
        sender: User?,
        type: NotificationType,
        message: String?,
        bookTitle: String?,
        bookImageUrl: String?,
        relatedId: Long?
    ): Notification {
        val notification = Notification.createNotification(
            receiver, sender, type, message, bookTitle, bookImageUrl, relatedId
        )

        return notificationRepository.save(notification)
    }

    // 알림 삭제
    @Transactional
    fun deleteNotification(notificationId: Long, user: User) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 알림입니다. ID: $notificationId") }

        // 본인의 알림인지 확인
        if (notification.receiver != user) {
            throw ServiceException("403-1", "다른 사용자의 알림을 삭제할 수 없습니다.")
        }

        notificationRepository.delete(notification)
    }

    /**
     * 대여 신청 상세 정보 조회
     * RENT_REQUEST 타입의 알림에 대한 상세 정보를 조회합니다.
     *
     * @param notificationId 알림 ID
     * @param user 현재 로그인한 사용자
     * @return 대여 신청 상세 정보 (rentListId 포함)
     */
    @Transactional(readOnly = true)
    fun getRentRequestDetail(notificationId: Long, user: User): Map<String, Any?> {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 알림입니다.") }

        // 본인의 알림인지 확인
        if (notification.receiver != user) {
            throw ServiceException("403-1", "다른 사용자의 알림에 접근할 수 없습니다.")
        }

        // RENT_REQUEST 타입인지 확인
        if (notification.type != NotificationType.RENT_REQUEST) {
            throw ServiceException("400-1", "대여 신청 알림이 아닙니다.")
        }

        // relatedId로 Rent 정보 조회 (relatedId는 rent.getId())
        val rentId = notification.relatedId
            ?: throw ServiceException("400-2", "알림에 연결된 대여 게시글 ID가 없습니다.")

        log.info("알림의 relatedId (rentId): {}", rentId)

        // Rent 정보 조회
        val rent = rentRepository.findById(rentId)
            .orElseThrow { ServiceException("404-2", "대여 게시글을 찾을 수 없습니다. ID: $rentId") }

        //  수정된 부분: 특정 알림의 발송자(신청자)와 일치하는 RentList 조회
        val requester = notification.sender // 알림을 발생시킨 사용자 (신청자)

        val detail = mutableMapOf<String, Any?>().apply {
            put("rentId", rent.id)
            put("bookTitle", rent.bookTitle)
            put("bookImage", rent.bookImage)
            put("rentStatus", rent.rentStatus?.description)
        }

        // 🆕 처리 가능 여부 추가
        var isProcessable = true
        var processStatus: String

        if (requester != null) {
            // 해당 신청자의 모든 RentList 조회 (상태 무관)
            val requesterRentLists = rentListRepository
                .findByRentIdAndBorrowerUserId(rentId, requester.id)

            if (requesterRentLists.isNotEmpty()) {
                // 가장 최근 신청의 상태 확인
                val latestRentList = requesterRentLists.last()
                
                detail["rentListId"] = latestRentList.id
                detail["requesterNickname"] = requester.nickname
                detail["requestDate"] = latestRentList.createdDate
                
                // ✅ RentList.kt로 변환되어 이제 직접 필드 접근 가능
                detail["loanDate"] = latestRentList.loanDate
                detail["returnDate"] = latestRentList.returnDate

                // ✅ 처리 상태 확인 - 직접 필드 접근
                val status = latestRentList.status
                when (status) {
                    RentRequestStatus.APPROVED -> {
                        isProcessable = false
                        processStatus = "APPROVED"
                    }

                    RentRequestStatus.REJECTED -> {
                        isProcessable = false
                        processStatus = "REJECTED"
                    }

                    else -> {
                        // 🆕 PENDING 상태여도 책이 이미 대여 중이면 처리 불가
                        if (rent.rentStatus == RentStatus.LOANED) {
                            isProcessable = false
                            processStatus = "BOOK_ALREADY_LOANED" // 다른 사람에게 대여됨
                        } else {
                            processStatus = "PENDING"
                        }
                    }
                }

                log.info(
                    "신청자 {}의 신청 상태: {}, 책 상태: {}, 처리가능: {}",
                    requester.nickname, status, rent.rentStatus, isProcessable
                )
            } else {
                // 신청 기록이 없는 경우
                detail["rentListId"] = null
                detail["requesterNickname"] = requester.nickname
                detail["requestDate"] = null
                detail["loanDate"] = null
                detail["returnDate"] = null
                isProcessable = false
                processStatus = "NOT_FOUND"
            }
        } else {
            // 시스템 알림 등으로 sender가 없는 경우
            log.warn("알림에 발송자 정보가 없음 - 알림 ID: {}", notificationId)
            detail["rentListId"] = null
            detail["requesterNickname"] = "알 수 없음"
            detail["requestDate"] = null
            detail["loanDate"] = null
            detail["returnDate"] = null
            isProcessable = false
            processStatus = "NO_SENDER"
        }

        //  처리 가능 여부 정보 추가
        detail["isProcessable"] = isProcessable
        detail["processStatus"] = processStatus

        log.info(
            "대여 신청 상세 정보 조회 완료 - 알림 ID: {}, Rent ID: {}, 신청자: {}, 처리가능: {}, 상태: {}",
            notificationId, rent.id, requester?.nickname ?: "없음", isProcessable, processStatus
        )

        return detail
    }
}
