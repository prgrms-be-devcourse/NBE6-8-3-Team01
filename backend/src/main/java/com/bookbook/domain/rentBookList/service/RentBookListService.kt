package com.bookbook.domain.rentBookList.service

import com.bookbook.domain.notification.enums.NotificationType
import com.bookbook.domain.notification.service.NotificationService
import com.bookbook.domain.rent.entity.Rent
import com.bookbook.domain.rentBookList.dto.RentBookListResponseDto
import com.bookbook.domain.rentBookList.repository.RentBookListRepository
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RentBookListService(
    private val rentBookListRepository: RentBookListRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {
    private val log = LoggerFactory.getLogger(RentBookListService::class.java)

    fun getAvailableBooks(
        page: Int,
        size: Int,
        region: String?,
        category: String?,
        search: String?
    ): Page<RentBookListResponseDto> {
        val pageable: Pageable = PageRequest.of(page, size)

        val rentPage: Page<Rent>

        // 필터링 조건이 있는지 확인
        val hasFilters = (region != null && region.trim().isNotEmpty() && region != "all") ||
                (category != null && category.trim().isNotEmpty() && category != "all") ||
                (search != null && search.trim().isNotEmpty())

        rentPage = if (hasFilters) {
            // 필터링 적용
            val regionFilter = if (region != null && region != "all") region else null
            val categoryFilter = if (category != null && category != "all") category else null
            val searchFilter = if (search != null && search.trim().isNotEmpty()) search else null

            rentBookListRepository.findAvailableBooks(regionFilter, categoryFilter, searchFilter, pageable)
        } else {
            // 전체 조회
            rentBookListRepository.findAllAvailableBooks(pageable)
        }

        return rentPage.map { rent ->
            // 사용자 닉네임 조회
            val lenderNickname = userRepository.findById(rent.lenderUserId!!)
                .map(User::nickname)
                .orElse("알 수 없음")
            RentBookListResponseDto(rent, lenderNickname)
        }
    }

    @Transactional
    fun requestRent(rentId: Long, message: String?) {
        // 대여 글 조회
        val rent = rentBookListRepository.findById(rentId)
            .orElseThrow { RuntimeException("존재하지 않는 대여 글입니다. ID: $rentId") }

        // 책 소유자 조회
        val bookOwner = userRepository.findById(rent.lenderUserId!!)
            .orElseThrow { RuntimeException("책 소유자를 찾을 수 없습니다. ID: ${rent.lenderUserId}") }

        // 신청자 조회 (현재는 임시로 admin 사용 - 실제로는 @AuthenticationPrincipal로 받아야 함)
        val requester = userRepository.findByUsername("admin")
            .orElseThrow { RuntimeException("신청자 정보를 찾을 수 없습니다.") }

        // 알림 생성
        notificationService.createNotification(
            bookOwner,
            requester,
            NotificationType.RENT_REQUEST,
            message,
            rent.bookTitle,
            rent.bookImage,
            rentId
        )

        log.info(
            "대여 신청 완료 - 대여글 ID: {}, 신청자: {}, 책 소유자: {}",
            rentId, requester.nickname, bookOwner.nickname
        )
    }

    fun getRegions(): List<Map<String, String>> {
        val regions = rentBookListRepository.findDistinctRegions()

        return regions.map { region ->
            mapOf(
                "id" to region,
                "name" to region
            )
        }
    }

    fun getCategories(): List<Map<String, String>> {
        val categories = rentBookListRepository.findDistinctCategories()

        return categories.map { category ->
            mapOf(
                "id" to category,
                "name" to category
            )
        }
    }

    fun getBookDetail(rentId: Long): RentBookListResponseDto {
        val rent = rentBookListRepository.findById(rentId)
            .orElseThrow { RuntimeException("존재하지 않는 책입니다. ID: $rentId") }

        // 사용자 닉네임 조회
        val lenderNickname = userRepository.findById(rent.lenderUserId!!)
            .map(User::nickname)
            .orElse("알 수 없음")

        return RentBookListResponseDto(rent, lenderNickname)
    }
}
