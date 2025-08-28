package com.bookbook.domain.lendList.service

import com.bookbook.domain.lendList.dto.LendListResponseDto
import com.bookbook.domain.rent.entity.RentStatus
import com.bookbook.domain.rent.repository.RentRepository
import com.bookbook.domain.rentList.repository.RentListRepository
import com.bookbook.domain.review.repository.ReviewRepository
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.exception.ServiceException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LendListService(
    private val rentRepository: RentRepository,
    private val rentListRepository: RentListRepository,
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository
) {
    /**
     * 사용자가 등록한 도서 목록 조회
     */
    fun getLendListByUserId(userId: Long, pageable: Pageable): Page<LendListResponseDto> {
        val rentPage = rentRepository.findFilteredRentHistory(
            pageable,
            listOf(RentStatus.AVAILABLE, RentStatus.LOANED, RentStatus.FINISHED),
            userId
        )
        
        return rentPage.map { rent ->
            val rentLists = rent.id?.let { rentListRepository.findByRentId(it) } ?: emptyList()
            val rentList = rentLists.firstOrNull()
            
            val borrowerNickname = rentList?.borrowerUser?.id?.let { borrowerId ->
                userRepository.findById(borrowerId)
                    .map { user -> user.nickname }
                    .orElse(null)
            }
            
            val hasReview = rentList?.let {
                rent.id?.let { rentId -> 
                    reviewRepository.findByRentIdAndReviewerId(rentId, userId).isPresent 
                }
            } ?: false
            
            LendListResponseDto(
                rent = rent,
                borrowerNickname = borrowerNickname,
                returnDate = rentList?.returnDate,
                hasReview = hasReview
            )
        }
    }

    /**
     * 사용자가 등록한 도서 목록 조회 (검색 기능 포함)
     */
    fun getLendListByUserIdAndSearch(userId: Long, search: String?, pageable: Pageable): Page<LendListResponseDto> {
        // 검색어와 관계없이 동일한 메서드 사용 (검색은 Controller에서 처리될 수도 있음)
        val rentPage = rentRepository.findFilteredRentHistory(
            pageable,
            listOf(RentStatus.AVAILABLE, RentStatus.LOANED, RentStatus.FINISHED),
            userId
        )
        
        return rentPage.map { rent ->
            val rentLists = rent.id?.let { rentListRepository.findByRentId(it) } ?: emptyList()
            val rentList = rentLists.firstOrNull()
            
            val borrowerNickname = rentList?.borrowerUser?.id?.let { borrowerId ->
                userRepository.findById(borrowerId)
                    .map { user -> user.nickname }
                    .orElse(null)
            }
            
            val hasReview = rentList?.let {
                rent.id?.let { rentId -> 
                    reviewRepository.findByRentIdAndReviewerId(rentId, userId).isPresent 
                }
            } ?: false
            
            LendListResponseDto(
                rent = rent,
                borrowerNickname = borrowerNickname,
                returnDate = rentList?.returnDate,
                hasReview = hasReview
            )
        }
    }

    /**
     * 완료된 도서 대여 목록 조회
     */
    fun getCompletedLendList(userId: Long, pageable: Pageable): Page<LendListResponseDto> {
        val rentPage = rentRepository.findFilteredRentHistory(
            pageable,
            listOf(RentStatus.FINISHED),
            userId
        )
        
        return rentPage.map { rent ->
            val rentLists = rent.id?.let { rentListRepository.findByRentId(it) } ?: emptyList()
            val rentList = rentLists.firstOrNull()
            
            val borrowerNickname = rentList?.borrowerUser?.id?.let { borrowerId ->
                userRepository.findById(borrowerId)
                    .map { user -> user.nickname }
                    .orElse(null)
            }
            
            val hasReview = rentList?.let {
                rent.id?.let { rentId -> 
                    reviewRepository.findByRentIdAndReviewerId(rentId, userId).isPresent 
                }
            } ?: false
            
            LendListResponseDto(
                rent = rent,
                borrowerNickname = borrowerNickname,
                returnDate = rentList?.returnDate,
                hasReview = hasReview
            )
        }
    }

    /**
     * 도서 게시글 삭제 (소프트 삭제)
     */
    @Transactional
    fun deleteLendList(userId: Long, rentId: Long) {
        val rent = rentRepository.findById(rentId)
            .orElseThrow {
                ServiceException(
                    "404-RENT-NOT-FOUND",
                    "해당 대여 게시글을 찾을 수 없습니다."
                )
            }
        
        // 작성자 확인
        if (rent.lenderUserId != userId) {
            throw ServiceException("403-FORBIDDEN", "해당 게시글을 삭제할 권한이 없습니다.")
        }
        
        // 소프트 삭제 (상태를 DELETED로 변경)
        rent.rentStatus = RentStatus.DELETED
        rentRepository.save(rent)
    }
}