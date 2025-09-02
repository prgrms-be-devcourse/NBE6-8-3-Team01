package com.bookbook.domain.suspend.service

import com.bookbook.domain.suspend.dto.request.UserSuspendRequestDto
import com.bookbook.domain.suspend.dto.response.UserSuspendResponseDto
import com.bookbook.domain.suspend.entity.SuspendedUser
import com.bookbook.domain.suspend.repository.SuspendedUserRepository
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.enums.UserStatus
import com.bookbook.domain.user.service.UserService
import com.bookbook.global.exception.ServiceException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 25.08.28 김지훈

@Service
class SuspendedUserService(
    private val suspendedUserRepository: SuspendedUserRepository,
    private val userService: UserService
) {

    /**
     * 유저를 정지시키고, 정지 히스토리에 추가합니다.
     *
     * @param requestDto 정지 요청 정보
     * @return 생성된 정지 유저에 대한 정보
     * @throws ServiceException (409) - 해당 유저가 이미 정지 중인 상태
     */
    @Transactional
    fun addUserAsSuspended(requestDto: UserSuspendRequestDto): SuspendedUser {
        val user = userService.findById(requestDto.userId)
            ?: throw ServiceException("404-1", "해당 유저는 존재하지 않습니다")

        if (user.isAdmin) throw ServiceException("403-1", "어드민은 정지시킬 수 없습니다.")

        // 현재 정지 중인지 확인하고 정지 중이면 중단
        checkUserIsSuspended(user)

        // 정지 상태로 전환 후 이력에 추가
        user.suspend(requestDto.period)

        val suspendedUser = SuspendedUser(user, requestDto.reason)

        return suspendedUserRepository.save(suspendedUser)
    }

    /**
     * 정지된 유저 히스토리를 페이지로 가져옵니다.
     *
     * @param page 페이지 번호
     * @param size 한 페이지 당 크기
     * @param userId 검색하려는 유저 Id
     * @return 유저 정지 히스토리 페이지
     */
    @Transactional(readOnly = true)
    fun getSuspendedHistoryPage(
        page: Int,
        size: Int,
        userId: Long? = null
    ): Page<UserSuspendResponseDto> {
        val pageable: Pageable = PageRequest.of(page - 1, size)

        return suspendedUserRepository
            .findAllFilteredUser(pageable, userId)
            .map { UserSuspendResponseDto(it) }
    }

    /**
     * 정지된 유저를 활동 재개 시킵니다.
     *
     * @param userId 정지 해제 대상 유저 ID
     * @return 갱신된 유저 정보
     * @throws ServiceException
     *
     * (404) 해당 유저가 존재하지 않을 때
     *
     * (409) 해당 유저가 이미 정지가 해제되었을 때
     */
    @Transactional
    fun resumeUser(userId: Long): User {
        val user = userService.findById(userId)
            ?: throw ServiceException("404-1", "존재하지 않는 사용자입니다.")

        checkUserIsActive(user)

        user.resume()

        return user
    }

    /**
     * 유저가 이미 정지가 해제되어 있는 지 확인합니다.
     *
     * @param user 유저 정보
     * @throws ServiceException (409) 해당 유저가 이미 활동 중일 때
     */
    private fun checkUserIsActive(user: User) {
        if (user.userStatus == UserStatus.ACTIVE)
            throw ServiceException("409-1", "해당 유저의 정지가 이미 해제되어 있습니다.")
    }

    /**
     * 유저가 이미 정지되어 있는 지 확인합니다.
     *
     * @param user 유저 정보
     * @throws ServiceException (409) 해당 유저가 이미 정지되어 있을 때
     */
    private fun checkUserIsSuspended(user: User) {
        if (user.isSuspended)
            throw ServiceException("409-1", "이 유저는 이미 정지 중입니다.")
    }
}
