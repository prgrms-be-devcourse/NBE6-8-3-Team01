package com.bookbook.domain.rent.service

import com.bookbook.domain.notification.enums.NotificationType
import com.bookbook.domain.notification.service.NotificationService
import com.bookbook.domain.rent.dto.request.RentRequestDto
import com.bookbook.domain.rent.dto.response.RentAvailableResponseDto
import com.bookbook.domain.rent.dto.response.RentDetailResponseDto
import com.bookbook.domain.rent.dto.response.RentResponseDto
import com.bookbook.domain.rent.dto.response.RentSimpleResponseDto
import com.bookbook.domain.rent.entity.Rent
import com.bookbook.domain.rent.entity.RentStatus
import com.bookbook.domain.rent.repository.RentRepository
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.domain.wishList.enums.WishListStatus
import com.bookbook.domain.wishList.repository.WishListRepository
import com.bookbook.global.exception.ServiceException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

// 25.08.28 현준
@Service
class RentService(
    private val rentRepository: RentRepository,
    private val notificationService: NotificationService,
    private val userRepository: UserRepository,
    private val wishListRepository: WishListRepository
) {

    // Rent 페이지 등록 Post 요청
    // /bookbook/rent/create
    @Transactional
    fun createRentPage(dto: RentRequestDto, userId: Long) {
        // 유저 정보 조회
        val user = userRepository.findById(userId)
            .orElseThrow { ServiceException("401", "로그인을 해 주세요.") }

        // Rent 엔티티 생성 (Named Parameters 활용)
        val rent = Rent(
            lenderUserId = userId,
            title = dto.title,
            bookCondition = dto.bookCondition,
            bookImage = dto.bookImage,
            address = dto.address,
            contents = dto.contents,
            rentStatus = dto.rentStatus,
            bookTitle = dto.bookTitle,
            author = dto.author,
            publisher = dto.publisher,
            category = dto.category,
            description = dto.description
        )

        // Rent 테이블에 추가
        val savedRent = rentRepository.save(rent)

        // 글 등록 알림 생성을 별도 트랜잭션으로 처리
        createNotificationSafely(user, dto, savedRent)
    }

    // 별도 트랜잭션으로 알림 생성 (실패해도 글 등록에 영향 없음)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun createNotificationSafely(user: User, dto: RentRequestDto, savedRent: Rent) {
        try {
            // 글을 등록한 사용자 본인에게만 확인용 알림 보내기
            notificationService.createNotification(
                user, // receiver (글을 등록한 사용자 본인)
                user, // sender (글을 등록한 사용자 본인) - 닉네임 표시용
                NotificationType.POST_CREATED, // POST_CREATED 타입 사용
                "도서 대여글이 성공적으로 등록되었습니다!",
                dto.bookTitle,
                dto.bookImage,
                savedRent.id
            )
        } catch (e: Exception) {
            // 알림 생성 실패 시 로그만 남기고 계속 진행
            System.err.println("❌ 알림 생성 실패 (하지만 글 등록은 성공): ${e.message}")
            e.printStackTrace()
        }
    }

    // Rent 페이지 조회 Get 요청
    // /bookbook/rent/{id}
    @Transactional(readOnly = true)
    fun getRentPage(id: Long, currentUserId: Long?): RentResponseDto {
        // 글 ID로 대여글 정보 조회
        val rent = rentRepository.findById(id)
            .orElseThrow { ServiceException("404-2", "해당 대여글을 찾을 수 없습니다.") }

        // ID로 대여자 정보 조회
        val rentUser = userRepository.findById(rent.lenderUserId)
            .orElseThrow { ServiceException("404-3", "해당 대여자를 찾을 수 없습니다.") }

        // 대여자가 작성한 글 갯수 조회
        val lenderPostCount = rentRepository.countByLenderUserId(rentUser.id ?: 0L)

        // 현재 사용자의 찜 상태 확인
        val isWishlisted = currentUserId?.let { userId ->
            wishListRepository.findByUserIdAndRentIdAndStatus(
                userId, id, WishListStatus.ACTIVE
            ) != null
        } ?: false

        return RentResponseDto(
            // 글 관련 정보
            id = rent.id,
            lenderUserId = rent.lenderUserId,
            title = rent.title,
            bookCondition = rent.bookCondition,
            bookImage = rent.bookImage,
            address = rent.address,
            contents = rent.contents,
            rentStatus = rent.rentStatus,

            // 책 관련 정보
            bookTitle = rent.bookTitle,
            author = rent.author,
            publisher = rent.publisher,
            category = rent.category,
            description = rent.description,
            createdDate = rent.createdDate,
            modifiedDate = rent.modifiedDate,

            // 글쓴이 정보
            nickname = rentUser.nickname,
            rating = rentUser.rating,
            lenderPostCount = lenderPostCount,

            // 찜 상태 정보
            isWishlisted = isWishlisted
        )
    }

    // Rent 페이지 수정 Put 요청
    // /bookbook/rent/edit/{id}
    @Transactional
    fun editRentPage(id: Long, dto: RentRequestDto, userId: Long) {
        // 글 ID로 대여글 정보 조회
        val rent = rentRepository.findById(id)
            .orElseThrow { ServiceException("404-2", "해당 대여글을 찾을 수 없습니다.") }

        // 글 작성자와 현재 로그인한 사용자가 일치하는지 확인
        isSameAuthor(userId, rent.lenderUserId )

        // Rent 엔티티 업데이트 (Kotlin의 mutable properties 활용)
        rent.apply {
            // 글 관련 정보
            title = dto.title
            bookCondition = dto.bookCondition
            bookImage = dto.bookImage
            address = dto.address
            contents = dto.contents
            rentStatus = dto.rentStatus

            // 책 관련 정보
            bookTitle = dto.bookTitle
            author = dto.author
            publisher = dto.publisher
            category = dto.category
            description = dto.description
        }

        // Rent 테이블에 업데이트
        rentRepository.save(rent)
    }

    // 대여 가능한 책 목록 조회 (필터링 및 페이지네이션 지원)
    @Transactional(readOnly = true)
    fun getAvailableBooks(
        region: String?,
        category: String?,
        search: String?,
        page: Int,
        size: Int
    ): RentAvailableResponseDto {
        // 페이지네이션 설정 (최신순 정렬)
        val pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdDate"))

        // 필터 조건 확인을 위한 extension functions
        fun String?.isValidFilter(): Boolean =
            this != null && this != "all" && this.trim().isNotEmpty()

        val hasRegion = region.isValidFilter()
        val hasCategory = category.isValidFilter()
        val hasSearch = search.isValidFilter()

        // 필터 조건에 따른 쿼리 실행 (when 표현식 활용)
        val rentPage = when {
            hasRegion && hasCategory && hasSearch ->
                rentRepository.findByRentStatusAndAddressAndCategoryAndSearchKeyword(
                    RentStatus.AVAILABLE, region!!.trim(), category!!.trim(), search!!.trim(), pageable
                )
            hasRegion && hasCategory ->
                rentRepository.findByRentStatusAndAddressContainingAndCategoryContaining(
                    RentStatus.AVAILABLE, region!!.trim(), category!!.trim(), pageable
                )
            hasRegion && hasSearch ->
                rentRepository.findByRentStatusAndAddressAndSearchKeyword(
                    RentStatus.AVAILABLE, region!!.trim(), search!!.trim(), pageable
                )
            hasCategory && hasSearch ->
                rentRepository.findByRentStatusAndCategoryAndSearchKeyword(
                    RentStatus.AVAILABLE, category!!.trim(), search!!.trim(), pageable
                )
            hasRegion ->
                rentRepository.findByRentStatusAndAddressContaining(
                    RentStatus.AVAILABLE, region!!.trim(), pageable
                )
            hasCategory ->
                rentRepository.findByRentStatusAndCategoryContaining(
                    RentStatus.AVAILABLE, category!!.trim(), pageable
                )
            hasSearch ->
                rentRepository.findByRentStatusAndSearchKeyword(
                    RentStatus.AVAILABLE, search!!.trim(), pageable
                )
            else ->
                rentRepository.findByRentStatus(RentStatus.AVAILABLE, pageable)
        }

        // 결과가 없는 경우
        if (rentPage.isEmpty) {
            return RentAvailableResponseDto.empty()
        }

        // Rent 엔티티를 BookInfo DTO로 변환 (사용자 닉네임 포함)
        val books = rentPage.content.map { rent ->
            // 사용자 닉네임 조회
            val lenderNickname = userRepository.findById(rent.lenderUserId ?: 0L)
                .map { it.nickname }
                .orElse("알 수 없음")

            RentAvailableResponseDto.BookInfo.from(rent, lenderNickname)
        }

        // 페이지네이션 정보 생성
        val pagination = RentAvailableResponseDto.PaginationInfo.from(rentPage)

        return RentAvailableResponseDto.success(books, pagination)
    }

    /**
     * 대여 게시글 목록을 페이지로 가져옵니다.
     *
     * @param pageable 페이지 기본 정보
     * @param status 대여 게시글 상태의 리스트
     * @param userId 대여 게시글 작성자 ID
     * @return 생성된 대여 게시글 페이지 정보
     */
    @Transactional(readOnly = true)
    fun getRentsPage(
        pageable: Pageable,
        status: List<RentStatus>?,
        userId: Long?
    ): Page<RentSimpleResponseDto> {
        return rentRepository.findFilteredRentHistory(pageable, status, userId)
            .map { RentSimpleResponseDto(it) }
    }

    /**
     * 대여 게시글의 상태를 변경합니다.
     *
     * @param rentId 대여 게시글 ID
     * @param newStatus 대여 게시글 상태 요청 본문
     * @return 수정된 대여 게시글 상세 정보
     */
    @Transactional
    fun modifyRentPageStatus(rentId: Long, newStatus: RentStatus): RentDetailResponseDto {
        val rent = rentRepository.findById(rentId)
            .orElseThrow { ServiceException("404-2", "해당 대여글을 찾을 수 없습니다.") }

        checkRentPostIsDeleted(rent)

        if (rent.rentStatus == newStatus) {
            throw ServiceException("409-1", "현재 상태와 동일합니다.")
        }

        rent.rentStatus = newStatus
        return RentDetailResponseDto(rent)
    }

    /**
     * 대여 게시글을 SOFT DELETE 합니다.
     *
     * @param userId 작성한 유저의 ID
     * @param rentId 대여 게시글의 ID
     * @throws ServiceException (404) 해당 대여 게시글이 존재하지 않을 때
     */
    @Transactional
    fun removeRentPage(userId: Long, rentId: Long) {
        val executor = userRepository.findById(userId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 유저입니다.") }

        val rent = rentRepository.findById(rentId)
            .orElseThrow { ServiceException("404-2", "해당 대여글을 찾을 수 없습니다.") }

        if (!executor.isAdmin) {
            isSameAuthor(userId, rent.lenderUserId)
        }

        checkRentPostIsDeleted(rent)

        rent.rentStatus = RentStatus.DELETED
    }

    /**
     * SOFT DELETE된 게시글을 복구합니다.
     * AVAILABLE 상태로 되돌아갑니다
     *
     * @param rentId 대여 게시글의 ID
     * @return 열람 가능 후 수정된 글의 상세 정보
     * @throws ServiceException (404) 해당 대여 게시글이 존재하지 않을 때
     */
    @Transactional
    fun restoreRentPage(rentId: Long): RentDetailResponseDto {
        val rent = rentRepository.findById(rentId)
            .orElseThrow { ServiceException("404-2", "해당 대여글을 찾을 수 없습니다.") }

        if (rent.rentStatus != RentStatus.DELETED) {
            throw ServiceException("409-1", "해당 글은 삭제된 상태가 아닙니다.")
        }

        rent.rentStatus = RentStatus.AVAILABLE

        return RentDetailResponseDto(rent)
    }

    /**
     * 대여 게시글에 대한 상세 정보를 가져옵니다.
     *
     * @param rentId 대여 게시글의 ID
     * @return 대여 게시글 기반으로 가공된 정보
     * @throws ServiceException (404) 해당 대여 게시글이 존재하지 않을 때
     */
    @Transactional(readOnly = true)
    fun getRentPostDetail(rentId: Long): RentDetailResponseDto {
        val rent = rentRepository.findById(rentId)
            .orElseThrow { ServiceException("404-2", "해당 대여글을 찾을 수 없습니다.") }

        return RentDetailResponseDto(rent)
    }

    /**
     * 대여 글 작성자와 요청자의 Id를 비교하여
     * 필요 시 예외를 throw 합니다.
     *
     * @param executorId 요청을 실행한 유저의 Id
     * @param authorId 게시글 작성자 Id
     * @throws ServiceException (403)
     */
    private fun isSameAuthor(executorId: Long, authorId: Long) {
        if (executorId != authorId)
            throw ServiceException("403", "해당 대여글을 수정할 권한이 없습니다.")
    }

    /**
     * 대여 게시글이 삭제되었는지 확인합니다.
     *
     * @param rent 대여 게시글의 객체
     * @throws ServiceException (404) 해당 대여 게시글이 삭제되었을 때
     */
    private fun checkRentPostIsDeleted(rent: Rent) {
        if (rent.rentStatus == RentStatus.DELETED) {
            throw ServiceException("404-1", "해당 글은 삭제되었습니다.")
        }
    }
}
