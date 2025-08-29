package com.bookbook.domain.report.controller

import com.bookbook.domain.report.dto.request.ReportRequestDto
import com.bookbook.domain.report.service.ReportService
import com.bookbook.global.exception.ServiceException
import com.bookbook.global.rsdata.RsData
import com.bookbook.global.security.CustomOAuth2User
import jakarta.validation.Valid
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/bookbook/reports")
class ReportController(
    private val reportService: ReportService
) {

    @PostMapping
    fun reportUser(
        @Valid @RequestBody requestDto: ReportRequestDto,
        @AuthenticationPrincipal customOAuth2User: CustomOAuth2User?
    ): ResponseEntity<RsData<Void>> {
        if (customOAuth2User == null || customOAuth2User.userId == -1L) {
            throw ServiceException("401-AUTH-INVALID", "로그인 정보가 유효하지 않습니다.")
        }
        val reporterUserId = customOAuth2User.userId

        reportService.createReport(reporterUserId, requestDto.targetUserId, requestDto.reason)

        val rsData: RsData<Void> = RsData(resultCode = "200-OK", msg = "신고가 성공적으로 접수되었습니다.", data = null)
        return ResponseEntity.status(rsData.statusCode).body(rsData)
    }
}