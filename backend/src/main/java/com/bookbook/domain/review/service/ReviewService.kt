package com.bookbook.domain.review.service

import com.bookbook.domain.rent.entity.Rent
import com.bookbook.domain.rent.entity.RentStatus
import com.bookbook.domain.rent.repository.RentRepository
import com.bookbook.domain.rentList.repository.RentListRepository
import com.bookbook.domain.review.dto.ReviewCreateRequestDto
import com.bookbook.domain.review.dto.ReviewResponseDto
import com.bookbook.domain.review.entity.Review
import com.bookbook.domain.review.repository.ReviewRepository
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 양방향 리뷰 관리 서비스
 *
 * 도서 대여 거래 완료 후 대여자와 대여받은 사람이 서로를 평가할 수 있는
 * 양방향 리뷰 시스템의 비즈니스 로직을 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val rentRepository: RentRepository,
    private val userRepository: UserRepository,
    private val rentListRepository: RentListRepository
) {

    /**
     * 대여자가 대여받은 사람에게 리뷰 작성
     *
     * 도서를 빌려준 사람이 빌려간 사람을 평가하는 기능입니다.
     * 거래 완료 후에만 작성 가능하며, 중복 리뷰는 방지됩니다.
     *
     * @param lenderId 대여자(리뷰 작성자) ID
     * @param rentId 대여 게시글 ID
     * @param request 리뷰 생성 요청 데이터 (평점)
     * @return 생성된 리뷰 정보
     * @throws ServiceException 게시글을 찾을 수 없거나 권한이 없는 경우
     * @throws ServiceException 거래가 완료되지 않았거나 이미 리뷰를 작성한 경우
     */
    @Transactional
    fun createLenderReview(lenderId: Long, rentId: Long, request: ReviewCreateRequestDto): ReviewResponseDto {
        // 대여 게시글 조회
        val rent = rentRepository.findById(rentId)
            .orElseThrow { ServiceException("404-1", "대여 게시글을 찾을 수 없습니다.") }

        // 본인이 작성한 글인지 확인
        if (rent.lenderUserId != lenderId) {
            throw ServiceException("403-1", "본인이 작성한 게시글에만 리뷰를 작성할 수 있습니다.")
        }

        // 공통 검증 로직 호출 - 중복 리뷰, 평점 유효성, 거래 완료 상태 확인
        validateReviewCreation(rent, lenderId, rentId, request.rating)

        // 빌려간 사람 ID 조회
        val rentList = rentListRepository.findByRentId(rentId)
            .first { it.rent.rentStatus == RentStatus.FINISHED }

        // 빌려간 사람의 ID 추출
        val borrowerId = rentList.borrowerUser.id

        // 리뷰 엔티티 생성 - 생성자를 통해 모든 정보 설정
        // "LENDER_TO_BORROWER": 대여자가 대여받은 사람을 평가하는 리뷰
        val review = Review(rentId, lenderId, borrowerId, request.rating, "LENDER_TO_BORROWER")

        //                         ↑       ↑         ↑           ↑                     ↑
        //                      대여글ID  작성자ID   대상자ID     평점                리뷰타입

        // 데이터베이스에 저장 - JPA가 INSERT 쿼리 실행
        val savedReview = reviewRepository.save(review)


        // 대상자(대여받은 사람)의 평균 평점 업데이트
        updateUserRating(borrowerId)

        // 추가 정보 조회
        val lenderNickname = userRepository.findById(lenderId)
            .map { it.nickname }
            .orElse("알 수 없음")
        val borrowerNickname = userRepository.findById(borrowerId)
            .map { it.nickname }
            .orElse("알 수 없음")

        return ReviewResponseDto(
            savedReview, lenderNickname, borrowerNickname,
            rent.bookTitle, rent.bookImage
        )
    }

    /**
     * 대여받은 사람이 대여자에게 리뷰 작성
     *
     * 도서를 빌린 사람이 빌려준 사람을 평가하는 기능입니다.
     * 거래 완료 후에만 작성 가능하며, 중복 리뷰는 방지됩니다.
     *
     * @param borrowerId 대여받은 사람(리뷰 작성자) ID
     * @param rentId 대여 게시글 ID
     * @param request 리뷰 생성 요청 데이터 (평점)
     * @return 생성된 리뷰 정보
     * @throws ServiceException 게시글을 찾을 수 없는 경우
     * @throws ServiceException 거래가 완료되지 않았거나 이미 리뷰를 작성한 경우
     */
    @Transactional
    fun createBorrowerReview(borrowerId: Long, rentId: Long, request: ReviewCreateRequestDto): ReviewResponseDto {
        // 대여 게시글 조회
        val rent = rentRepository.findById(rentId)
            .orElseThrow { ServiceException("404-1", "대여 게시글을 찾을 수 없습니다.") }

        // 공통 검증 로직 호출
        validateReviewCreation(rent, borrowerId, rentId, request.rating)

        // 본인이 해당 도서를 대여했는지 확인
        val isBorrower = rentListRepository.findByRentId(rentId)
            .any { it.borrowerUser.id == borrowerId }

        if (!isBorrower) {
            throw ServiceException("403-1", "해당 도서를 대여한 사용자만 리뷰를 작성할 수 있습니다.")
        }

        // TODO: Rent 엔티티에서 nullable이 제거되면 !! 연산자 제거
        // 리뷰 생성 (빌려간 사람이 빌려준 사람을 평가)
        val review = Review(rentId, borrowerId, rent.lenderUserId!!, request.rating, "BORROWER_TO_LENDER")
        val savedReview = reviewRepository.save(review)

        // 사용자 평점 업데이트
        updateUserRating(rent.lenderUserId!!)

        // 추가 정보 조회
        val borrowerNickname = userRepository.findById(borrowerId)
            .map { it.nickname }
            .orElse("알 수 없음")
        val lenderNickname = userRepository.findById(rent.lenderUserId!!)
            .map { it.nickname }
            .orElse("알 수 없음")

        return ReviewResponseDto(
            savedReview, borrowerNickname, lenderNickname,
            rent.bookTitle, rent.bookImage
        )
    }

    /**
     * 리뷰 작성 공통 검증 로직
     *
     * @param rent 대여 게시글
     * @param reviewerId 리뷰 작성자 ID
     * @param rentId 대여 게시글 ID
     * @param rating 평점
     */
    private fun validateReviewCreation(rent: Rent, reviewerId: Long, rentId: Long, rating: Int) {
        // 거래 완료 상태 확인 - FINISHED 상태일 때만 리뷰 작성 가능
        if (rent.rentStatus != RentStatus.FINISHED) {
            throw ServiceException("400-1", "거래가 완료된 경우에만 리뷰를 작성할 수 있습니다.")
        }

        // 중복 리뷰 방지 - 같은 사람이 같은 대여 건에 이미 리뷰 작성했는지 확인
        val existingReview = reviewRepository.findByRentIdAndReviewerId(rentId, reviewerId)
        // Optional.isPresent(): Optional에 값이 있으면 true (= 이미 리뷰 존재)
        if (existingReview.isPresent) {
            throw ServiceException("409-1", "이미 리뷰를 작성하셨습니다.")
        }

        // 평점 유효성 검사 - 1~5점 범위 확인
        // 비즈니스 룰: 별점은 1점(최저) ~ 5점(최고) 사이만 허용
        if (rating !in 1..5) {
            throw ServiceException("400-2", "별점은 1점부터 5점까지 입력 가능합니다.")
        }
    }

    /**
     * 사용자 평점 업데이트
     *
     * 리뷰 작성 후 해당 사용자의 평균 평점을 계산하여 사용자 정보를 업데이트합니다.
     *
     * @param userId 평점을 업데이트할 사용자 ID
     */
    private fun updateUserRating(userId: Long) {
        val averageRating = reviewRepository.findAverageRatingByRevieweeId(userId)
        if (averageRating.isPresent) {
            val user = userRepository.findById(userId)
                .orElseThrow { ServiceException("404-1", "사용자를 찾을 수 없습니다.") }
            user.changeRating(averageRating.get().toFloat())
            userRepository.save(user)
        }
    }
}