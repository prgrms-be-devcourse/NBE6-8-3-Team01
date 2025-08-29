package com.bookbook.domain.notification.controller

import com.bookbook.domain.notification.dto.NotificationResponseDto
import com.bookbook.domain.notification.service.NotificationService
import com.bookbook.domain.user.service.UserService
import com.bookbook.global.rsdata.RsData
import com.bookbook.global.security.CustomOAuth2User
import io.swagger.v3.oas.annotations.Operation
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

//08-29 유효상
@RestController
@RequestMapping("/api/v1/bookbook/user/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val userService: UserService
) {
    private val log = LoggerFactory.getLogger(NotificationController::class.java)

    @Transactional(readOnly = true)
    @GetMapping(produces = ["application/json"])
    @Operation(summary = "사용자 알림 조회", description = "현재 로그인한 사용자의 모든 알림을 최신순으로 조회합니다.")
    fun getNotifications(
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): RsData<List<NotificationResponseDto>?> {
        log.info("===== 알림 조회 API 호출됨 =====")

        // 로그인하지 않은 경우
        if (customOAuth2User == null) {
            log.warn("customOAuth2User가 null입니다.")
            return RsData("401-1", "로그인 후 사용해주세요.")
        }

        if (customOAuth2User.userId == null) {
            log.warn("customOAuth2User.getUserId()가 null입니다.")
            return RsData("401-1", "로그인 후 사용해주세요.")
        }

        log.info(
            "사용자 알림 조회 요청: 사용자 ID = {}, 사용자명 = {}",
            customOAuth2User.userId, customOAuth2User.name
        )

        return try {
            // CustomOAuth2User에서 실제 User 엔티티를 조회
            val user = userService.findById(customOAuth2User.userId)
            if (user == null) {
                log.error("사용자 ID {}에 해당하는 사용자를 찾을 수 없습니다.", customOAuth2User.userId)
                return RsData("404-1", "사용자 정보를 찾을 수 없습니다.")
            }

            log.info("사용자 조회 성공: {}", user.nickname)

            val notifications = notificationService.getNotificationsByUser(user)
            log.info("알림 조회 성공: {} 개의 알림 반환", notifications.size)

            val response: RsData<List<NotificationResponseDto>?> = RsData("200-1", "알림 목록을 조회했습니다.", notifications)
            log.info(
                "응답 데이터 생성 완료: resultCode={}, dataSize={}",
                response.resultCode,
                notifications.size
            )

            response
        } catch (e: Exception) {
            log.error("알림 조회 중 오류 발생: {}", e.message, e)
            RsData("500-1", "알림 조회 중 오류가 발생했습니다.")
        }
    }

    @Transactional(readOnly = true)
    @GetMapping(value = ["/unread-count"], produces = ["application/json"])
    @Operation(summary = "읽지 않은 알림 개수", description = "현재 사용자의 읽지 않은 알림 개수를 조회합니다.")
    fun getUnreadCount(
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): RsData<Long?> {
        if (customOAuth2User?.userId == null) {
            return RsData("401-1", "로그인 후 사용해주세요.")
        }

        val user = userService.findById(customOAuth2User.userId)
            ?: return RsData("404-1", "사용자 정보를 찾을 수 없습니다.")

        val count = notificationService.getUnreadCount(user)
        return RsData("200-1", "읽지 않은 알림 개수를 조회했습니다.", count)
    }

    @Transactional
    @PatchMapping(value = ["/{id}/read"], produces = ["application/json"])
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    fun markAsRead(
        @PathVariable id: Long,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): RsData<Void?> {
        if (customOAuth2User?.userId == null) {
            return RsData("401-1", "로그인 후 사용해주세요.")
        }

        val user = userService.findById(customOAuth2User.userId)
            ?: return RsData("404-1", "사용자 정보를 찾을 수 없습니다.")

        return try {
            notificationService.markAsRead(id, user)
            RsData("200-1", "알림을 읽음 처리했습니다.")
        } catch (e: RuntimeException) {
            log.error("알림 읽음 처리 실패: {}", e.message)
            RsData("400-1", e.message ?: "알림 읽음 처리 중 오류가 발생했습니다.")
        }
    }

    @Transactional
    @PatchMapping(value = ["/read-all"], produces = ["application/json"])
    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 변경합니다.")
    fun markAllAsRead(
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): RsData<Void?> {
        if (customOAuth2User?.userId == null) {
            return RsData("401-1", "로그인 후 사용해주세요.")
        }

        val user = userService.findById(customOAuth2User.userId)
            ?: return RsData("404-1", "사용자 정보를 찾을 수 없습니다.")

        notificationService.markAllAsRead(user)
        return RsData("200-1", "모든 알림을 읽음 처리했습니다.")
    }

    @Transactional
    @DeleteMapping(value = ["/{id}"], produces = ["application/json"])
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    fun deleteNotification(
        @PathVariable id: Long,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): RsData<Void?> {
        if (customOAuth2User?.userId == null) {
            return RsData("401-1", "로그인 후 사용해주세요.")
        }

        val user = userService.findById(customOAuth2User.userId)
            ?: return RsData("404-1", "사용자 정보를 찾을 수 없습니다.")

        return try {
            notificationService.deleteNotification(id, user)
            RsData("200-1", "알림을 삭제했습니다.")
        } catch (e: RuntimeException) {
            log.error("알림 삭제 실패: {}", e.message)
            RsData("400-1", e.message ?: "알림 삭제 중 오류가 발생했습니다.")
        }
    }

    /**
     * 대여 신청에 대한 상세 정보 조회
     *
     * @param id 알림 ID
     * @param customOAuth2User 현재 로그인한 사용자
     * @return 대여 신청 상세 정보
     */
    @GetMapping("/{id}/rent-request-detail")
    @Operation(summary = "대여 신청 상세 정보 조회", description = "알림의 대여 신청에 대한 상세 정보를 조회합니다.")
    fun getRentRequestDetail(
        @PathVariable id: Long,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): RsData<Map<String, Any?>> {
        log.info("===== 대여 신청 상세 정보 조회 API 호출 - 알림 ID: {} =====", id)

        if (customOAuth2User?.userId == null) {
            log.warn("인증되지 않은 사용자의 접근 시도")
            return RsData("401-1", "로그인 후 사용해주세요.", emptyMap())
        }

        val user = userService.findById(customOAuth2User.userId)
        if (user == null) {
            log.error("사용자 ID {}에 해당하는 사용자를 찾을 수 없습니다.", customOAuth2User.userId)
            return RsData("404-1", "사용자 정보를 찾을 수 없습니다.", emptyMap())
        }

        return try {
            val detail = notificationService.getRentRequestDetail(id, user)
            log.info("대여 신청 상세 정보 조회 성공 - 알림 ID: {}, 응답 데이터: {}", id, detail)
            RsData("200-1", "대여 신청 상세 정보를 조회했습니다.", detail)
        } catch (e: RuntimeException) {
            log.error("대여 신청 상세 정보 조회 실패 - 알림 ID: {}, 오류: {}", id, e.message, e)
            RsData("400-1", e.message ?: "대여 신청 상세 정보 조회 중 오류가 발생했습니다.", emptyMap())
        }
    }
}
