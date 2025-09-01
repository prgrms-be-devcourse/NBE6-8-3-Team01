package com.bookbook.domain.rentList.service

import com.bookbook.domain.notification.enums.NotificationType
import com.bookbook.domain.notification.service.NotificationService
import com.bookbook.domain.rent.entity.RentStatus
import com.bookbook.domain.rent.repository.RentRepository
import com.bookbook.domain.rentList.dto.RentListCreateRequestDto
import com.bookbook.domain.rentList.dto.RentListResponseDto
import com.bookbook.domain.rentList.dto.RentRequestDecisionDto
import com.bookbook.domain.rentList.entity.RentList
import com.bookbook.domain.rentList.entity.RentRequestStatus
import com.bookbook.domain.rentList.repository.RentListRepository
import com.bookbook.domain.review.repository.ReviewRepository
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 내가 빌린 도서 목록 관리 서비스
 * 
 * 사용자의 도서 대여 신청, 대여 목록 조회 등의 비즈니스 로직을 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class RentListService(
    private val rentListRepository: RentListRepository,
    private val userRepository: UserRepository,
    private val rentRepository: RentRepository,
    private val notificationService: NotificationService,
    private val reviewRepository: ReviewRepository
) {
    
    companion object {
        private val log = LoggerFactory.getLogger(RentListService::class.java)
    }

    /**
     * 사용자가 대여한 도서 목록 조회
     * 
     * @param borrowerUserId 대여받은 사용자 ID
     * @return 대여한 도서 목록
     */
    fun getRentListByUserId(borrowerUserId: Long): List<RentListResponseDto> {
        return rentListRepository.findByBorrowerUserIdOrderByCreatedDateDesc(borrowerUserId)
            .map { toRentListResponseDto(it, borrowerUserId) }
    }
    
    /**
     * 사용자가 대여한 도서 목록 검색
     * 
     * @param borrowerUserId 대여받은 사용자 ID
     * @param searchKeyword 검색어 (책 제목, 저자, 출판사, 게시글 제목에서 검색)
     * @return 검색된 대여한 도서 목록
     */
    fun searchRentListByUserId(borrowerUserId: Long, searchKeyword: String?): List<RentListResponseDto> {
        val rentLists = rentListRepository.findByBorrowerUserIdOrderByCreatedDateDesc(borrowerUserId)
        
        return if (searchKeyword.isNullOrBlank()) {
            rentLists.map { toRentListResponseDto(it, borrowerUserId) }
        } else {
            val searchLower = searchKeyword.lowercase().trim()
            rentLists
                .filter { matchesSearchKeyword(it.rent, searchLower) }
                .map { toRentListResponseDto(it, borrowerUserId) }
        }
    }

    /**
     * 도서 반납하기
     *
     * 대여받은 사람이 도서를 조기 반납하는 기능입니다.
     * 반납 시 해당 대여 기록과 원본 게시글의 상태를 업데이트합니다.
     *
     * @param borrowerUserId 대여받은 사용자 ID
     * @param rentId 대여 게시글 ID
     * @throws IllegalArgumentException 대여 기록을 찾을 수 없거나 이미 반납된 경우
     */
    @Transactional
    fun returnBook(borrowerUserId: Long, rentId: Long) {
        // 해당 사용자의 진행 중인 대여 기록 조회 (APPROVED 상태만)
        val rentLists = rentListRepository.findByRentIdAndBorrowerUserIdAndStatus(
            rentId, borrowerUserId, RentRequestStatus.APPROVED
        )

        if (rentLists.isEmpty()) {
            throw ServiceException("404-1", "진행 중인 대여 기록을 찾을 수 없습니다.")
        }
        if (rentLists.size > 1) {
            throw ServiceException("500-1", "여러 개의 진행 중인 대여 기록이 발견되었습니다. 관리자에게 문의하세요.")
        }

        val rentList = rentLists.first()
        val rent = rentList.rent

        // 이미 반납된 상태인지 확인
        if (rent.rentStatus == RentStatus.FINISHED) {
            throw ServiceException("400-1", "이미 반납된 도서입니다.")
        }

        // 반납 완료 처리
        rentList.status = RentRequestStatus.FINISHED
        rent.rentStatus = RentStatus.FINISHED

        // 변경사항 저장
        rentListRepository.save(rentList)
        rentRepository.save(rent)
    }

    /**
     * RentList를 RentListResponseDto로 변환
     */
    private fun toRentListResponseDto(rentList: RentList, borrowerUserId: Long): RentListResponseDto {
        val lenderNickname = userRepository.findById(rentList.rent.lenderUserId!!)
            .map { it.nickname }
            .orElse("알 수 없음")

        val hasReview = reviewRepository.findByRentIdAndReviewerId(rentList.rent.id, borrowerUserId)
            .isPresent

        return RentListResponseDto(rentList, lenderNickname, hasReview)
    }

    /**
     * 검색 키워드와 매칭 여부 확인
     */
    private fun matchesSearchKeyword(rent: com.bookbook.domain.rent.entity.Rent, searchLower: String): Boolean {
        return listOf(rent.bookTitle, rent.author, rent.publisher, rent.title)
            .any { it?.lowercase()?.contains(searchLower) == true }
    }
    
    /**
     * 도서 대여 신청 등록
     * 
     * 사용자가 원하는 도서에 대해 대여 신청을 등록합니다.
     * 반납일은 대여일로부터 자동으로 14일 후로 설정됩니다.
     * 
     * @param borrowerUserId 대여받을 사용자 ID
     * @param request 대여 신청 정보
     * @throws IllegalArgumentException 사용자나 게시글을 찾을 수 없는 경우
     */
    @Transactional
    fun createRentList(borrowerUserId: Long, request: RentListCreateRequestDto) {
        // User 엔티티 조회
        val borrowerUser = userRepository.findById(borrowerUserId)
            .orElseThrow { ServiceException("404-1", "사용자를 찾을 수 없습니다. userId: $borrowerUserId") }
        
        // Rent 엔티티 조회
        val rent = rentRepository.findById(request.rentId)
            .orElseThrow { ServiceException("404-2", "대여 게시글을 찾을 수 없습니다. rentId: ${request.rentId}") }
        
        // 중복 신청 방지 로직
        val alreadyRequested = rentListRepository
            .existsByBorrowerUserIdAndRentIdAndStatus(borrowerUserId, request.rentId, RentRequestStatus.PENDING)
        
        if (alreadyRequested) {
            log.warn("중복 대여 신청 차단 - 사용자: $borrowerUserId, Rent ID: ${request.rentId}")
            throw ServiceException("400-1", "이미 대여 신청을 하셨습니다. 승인 결과를 기다려주세요.")
        }
        
        // 자신의 책에 신청하는 것 방지
        if (rent.lenderUserId == borrowerUserId) {
            log.warn("자신의 책 대여 신청 차단 - 사용자: $borrowerUserId, Rent ID: ${request.rentId}")
            throw ServiceException("400-2", "자신의 책은 대여 신청할 수 없습니다.")
        }
        
        // 이미 대여 중인 책인지 확인
        if (rent.rentStatus == RentStatus.LOANED) {
            log.warn("이미 대여 중인 책 신청 차단 - Rent ID: ${request.rentId}, 상태: ${rent.rentStatus}")
            throw ServiceException("400-3", "이미 대여 중인 책입니다.")
        }
        
        // 새로운 대여 기록 생성
        val rentList = RentList(
            loanDate = request.loanDate,
            returnDate = request.loanDate.plusDays(14)
        ).apply {
            this.borrowerUser = borrowerUser
            this.rent = rent
        }
        
        rentListRepository.save(rentList)
        
        // 책 소유자에게 대여 신청 알림 발송
        val lender = userRepository.findById(rent.lenderUserId!!)
            .orElseThrow { ServiceException("404-3", "책 소유자를 찾을 수 없습니다.") }
        
        val requestMessage = "'${rent.bookTitle}'에 대여 요청이 도착했어요!"
        notificationService.createNotification(
            lender,
            borrowerUser,
            NotificationType.RENT_REQUEST,
            requestMessage,
            rent.bookTitle,
            rent.bookImage,
            rent.id
        )
        
        log.info(
            "대여 신청 완료 및 알림 발송 - 책: {}, 신청자: {}, 소유자: {}",
            rent.bookTitle, borrowerUser.nickname, lender.nickname
        )
    }

    /**
     * 대여 신청 수락/거절 처리
     * 
     * @param rentListId 대여 신청 ID
     * @param decision 수락/거절 결정 정보
     * @param currentUser 현재 로그인한 사용자 (책 소유자)
     * @return 처리 결과 메시지
     */
    @Transactional
    fun decideRentRequest(rentListId: Long, decision: RentRequestDecisionDto, currentUser: User): String {
        // 대여 신청 조회
        val rentList = rentListRepository.findById(rentListId)
            .orElseThrow { ServiceException("404-1", "대여 신청을 찾을 수 없습니다.") }
        
        val rent = rentList.rent
        
        // 권한 확인: 현재 사용자가 책 소유자인지 확인
        if (rent.lenderUserId != currentUser.id) {
            throw ServiceException("403-1", "해당 대여 신청을 처리할 권한이 없습니다.")
        }
        
        // 이미 처리된 신청인지 확인
        if (rentList.status != RentRequestStatus.PENDING) {
            throw ServiceException("400-1", "이미 처리된 대여 신청입니다.")
        }
        
        val borrower = rentList.borrowerUser
        
        return if (decision.approved) {
            // 수락 처리
            rentList.status = RentRequestStatus.APPROVED
            rent.rentStatus = RentStatus.LOANED
            
            rentListRepository.save(rentList)
            rentRepository.save(rent)
            
            // 같은 책에 대한 다른 모든 PENDING 신청들을 자동으로 거절 처리
            val otherPendingRequests = rentListRepository.findByRentIdAndStatus(rent.id, RentRequestStatus.PENDING)

            otherPendingRequests
                .filter { it.id != rentListId }
                .forEach { otherRequest ->
                    otherRequest.status = RentRequestStatus.REJECTED
                    rentListRepository.save(otherRequest)

                    val otherBorrower = otherRequest.borrowerUser
                    val rejectMessage = "'${rent.bookTitle}' 대여 요청이 거절되었습니다."
                    notificationService.createNotification(
                        otherBorrower,
                        null,
                        NotificationType.RENT_REJECTED,
                        rejectMessage,
                        rent.bookTitle,
                        rent.bookImage,
                        rent.id.toLong()
                    )
                    
                    log.info(
                        "다른 신청자 자동 거절 처리 - 신청자: {}, 사유: 다른 사용자 수락됨", 
                        otherBorrower.nickname
                    )
                }
            
            // 신청자에게 수락 알림 발송
            val approveMessage = "'${rent.bookTitle}' 대여 요청이 수락되었습니다!"
            notificationService.createNotification(
                borrower,
                currentUser,
                NotificationType.RENT_APPROVED,
                approveMessage,
                rent.bookTitle,
                rent.bookImage,
                rent.id
            )
            
            // 기존 RENT_REQUEST 알림을 처리 완료로 표시
            notificationService.markRentRequestAsProcessed(rent.id, currentUser)
            
            log.info(
                "대여 신청 수락 완료 - 책: {}, 대여자: {}, 신청자: {}, 자동 거절된 다른 신청: {}개", 
                rent.bookTitle, currentUser.nickname, borrower.nickname, 
                otherPendingRequests.size - 1
            )
            
            "대여 신청을 수락했습니다."
            
        } else {
            // 거절 처리
            rentList.status = RentRequestStatus.REJECTED
            rentListRepository.save(rentList)
            
            // 신청자에게 거절 알림 발송
            val rejectMessage = "'${rent.bookTitle}' 대여 요청이 거절되었습니다."
            val detailMessage = decision.rejectionReason?.takeIf { it.isNotBlank() }
                ?: "죄송합니다. 대여 요청을 수락할 수 없습니다."
            
            notificationService.createNotification(
                borrower,
                currentUser,
                NotificationType.RENT_REJECTED,
                rejectMessage,
                rent.bookTitle,
                rent.bookImage,
                rent.id
            )
            
            // 기존 RENT_REQUEST 알림을 처리 완료로 표시
            notificationService.markRentRequestAsProcessed(rent.id, currentUser)
            
            log.info(
                "대여 신청 거절 완료 - 책: {}, 대여자: {}, 신청자: {}, 사유: {}", 
                rent.bookTitle, currentUser.nickname, borrower.nickname, detailMessage
            )
            
            "대여 신청을 거절했습니다."
        }
    }
}