package com.bookbook.domain.user.service

import com.bookbook.domain.user.dto.response.UserDetailResponseDto
import com.bookbook.domain.user.dto.response.UserSimpleResponseDto
import com.bookbook.domain.user.entity.User
import com.bookbook.domain.user.enums.UserStatus
import com.bookbook.domain.user.repository.UserRepository
import com.bookbook.global.exception.ServiceException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userService: UserService
) {

    /**
     * 로그인을 검증 로직입니다.
     *
     * @param username 유저명
     * @param password 비밀번호
     * @return 확인된 유저 정보
     * @throws ServiceException
     *
     *
     * (401) - 로그인 유저가 어드민이 아닐 때
     * (404) - 조건에 맞는 유저를 찾지 못했을 때
     *
     */
    @Transactional(readOnly = true)
    fun login(username: String, password: String): User {
        val user = findByUsername(username)

        checkPassword(user, password)

        if (!user.isAdmin) throw ServiceException("401-UNAUTHORIZED", "허가되지 않은 접근입니다.")

        return user
    }

    /**
     * 유저 정보를 페이지로 가져옵니다
     *
     * @param pageable 페이지 정보
     * @param status 유저 상태 리스트
     * @param userId 정보를 검색할 유저 ID
     * @return 필터링된 유저 정보 페이지
     */
    @Transactional(readOnly = true)
    fun getFilteredUsers(
        pageable: Pageable, status: List<UserStatus>?, userId: Long?
    ): Page<UserSimpleResponseDto> {
        return userRepository.findFilteredUsers(pageable, status, userId)
            .map { UserSimpleResponseDto(it) }
    }

    /**
     * 특정 유저 정보를 가져옵니다
     *
     * @param userId 페이지 정보
     * @return 유저 상세 정보
     * @throws ServiceException (404) 조건에 맞는 유저 정보를 찾지 못했을 때
     */
    @Transactional(readOnly = true)
    fun getSpecificUserInfo(userId: Long): UserDetailResponseDto {
        val user = userService.findById(userId)
            ?: throw ServiceException("404-1", "존재하지 않는 유저입니다.")

        return UserDetailResponseDto(user)
    }

    /**
     * 유저명으로 정보를 가져옵니다
     *
     * @param username 유저명
     * @return 유저 상세 정보
     * @throws ServiceException (404) 조건에 맞는 유저 정보를 찾지 못했을 때
     */
    private fun findByUsername(username: String): User {
        return userRepository.findByUsername(username)
            .orElseThrow { ServiceException("404-USER-NOT-FOUND", "존재하지 않는 유저입니다.") }
    }

    /**
     * 유저의 비밀번호화 요청 입력 비밀번호를 비교합니다.
     *
     * @param user 페이지 정보
     * @param password 로그인 시 입력된 패스워드 정보
     * @throws ServiceException (404) 조건에 맞는 유저 정보를 찾지 못했을 때
     */
    private fun checkPassword(user: User, password: String) {
        if (!passwordEncoder.matches(password, user.password)) {
            throw ServiceException("404-USER-NOT-FOUND", "존재하지 않는 유저입니다.")
        }
    }
}
