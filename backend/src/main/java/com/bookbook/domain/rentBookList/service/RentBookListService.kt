package com.bookbook.domain.rentBookList.service

import com.bookbook.domain.notification.enums.NotificationType
import com.bookbook.domain.notification.service.NotificationService
import com.bookbook.domain.rent.entity.Rent
import com.bookbook.domain.rentBookList.dto.RentBookListResponseDto
import com.bookbook.domain.rentBookList.repository.RentBookListRepository
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.exception.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 대여 가능한 책 목록 관련 서비스
@Service
@Transactional(readOnly = true)
class RentBookListService(
    private val rentBookListRepository: RentBookListRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {
    private val log = LoggerFactory.getLogger(RentBookListService::class.java)

    // 대여 가능한 책 목록 조회
    fun getAvailableBooks(
        page: Int,
        size: Int,
        region: String?,
        category: String?,
        search: String?
    ): Page<RentBookListResponseDto> {
        val pageable: Pageable = PageRequest.of(page, size)
        
        val hasFilters = (region != null && region.trim().isNotEmpty() && region != "all") ||
                (category != null && category.trim().isNotEmpty() && category != "all") ||
                (search != null && search.trim().isNotEmpty())

        val rentPage = if (hasFilters) {
            val regionFilter = if (region != null && region != "all") region else null
            val categoryFilter = if (category != null && category != "all") category else null
            val searchFilter = if (search != null && search.trim().isNotEmpty()) search else null

            rentBookListRepository.findAvailableBooks(regionFilter, categoryFilter, searchFilter, pageable)
        } else {
            rentBookListRepository.findAllAvailableBooks(pageable)
        }

        return rentPage.map { rent ->
            val lenderNickname = userRepository.findById(rent.lenderUserId!!)
                .map(User::nickname)
                .orElse("알 수 없음")
            RentBookListResponseDto(rent, lenderNickname)
        }
    }

    // 대여 신청
    @Transactional
    fun requestRent(rentId: Long, message: String?) {
        val rent = rentBookListRepository.findById(rentId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 대여 글입니다.") }

        val bookOwner = userRepository.findById(rent.lenderUserId!!)
            .orElseThrow { ServiceException("404-2", "책 소유자를 찾을 수 없습니다.") }

        val requester = userRepository.findByUsername("admin")
            .orElseThrow { ServiceException("404-3", "신청자 정보를 찾을 수 없습니다.") }

        notificationService.createNotification(
            bookOwner,
            requester,
            NotificationType.RENT_REQUEST,
            message,
            rent.bookTitle,
            rent.bookImage,
            rentId
        )

        log.info("대여 신청 완료 - rentId: {}, requester: {}, owner: {}", 
            rentId, requester.nickname, bookOwner.nickname)
    }

    // 지역 목록 조회
    fun getRegions(): List<Map<String, String>> {
        val regions = rentBookListRepository.findDistinctRegions()
        return regions.map { region ->
            mapOf("id" to region, "name" to region)
        }
    }

    // 카테고리 목록 조회
    fun getCategories(): List<Map<String, String>> {
        val categories = rentBookListRepository.findDistinctCategories()
        return categories.map { category ->
            mapOf("id" to category, "name" to category)
        }
    }

    // 책 상세 정보 조회
    fun getBookDetail(rentId: Long): RentBookListResponseDto {
        val rent = rentBookListRepository.findById(rentId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 책입니다.") }

        val lenderNickname = userRepository.findById(rent.lenderUserId!!)
            .map(User::nickname)
            .orElse("알 수 없음")

        return RentBookListResponseDto(rent, lenderNickname)
    }
}
