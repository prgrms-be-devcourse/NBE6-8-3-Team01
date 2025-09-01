package com.bookbook.domain.notification.dto

import com.bookbook.domain.notification.entity.Notification
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

//08-29 유효상
data class NotificationResponseDto(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("message")
    val message: String?,

    @JsonProperty("time")
    val time: String?,

    @JsonProperty("read")
    val read: Boolean = false,

    @JsonProperty("processed")
    val processed: Boolean = false,

    @JsonProperty("bookTitle")
    val bookTitle: String?,

    @JsonProperty("detailMessage")
    val detailMessage: String?,

    @JsonProperty("imageUrl")
    val imageUrl: String?,

    @JsonProperty("requester")
    val requester: String?,

    @JsonProperty("type")
    val type: String?,

    @JsonProperty("rentId")
    val rentId: Long? // rent ID 추가
) {
    companion object {
        fun from(notification: Notification): NotificationResponseDto {
            return NotificationResponseDto(
                id = notification.id,
                message = notification.title, // enum에서 가져온 기본 메시지
                time = formatTime(notification.createdDate),
                read = notification.isRead,
                processed = notification.isProcessed, // 처리 완료 여부 추가
                bookTitle = notification.bookTitle ?: "",
                detailMessage = notification.message ?: "", // 상세 메시지
                imageUrl = formatImageUrl(notification.bookImageUrl), // 이미지 URL 포맷팅
                requester = notification.sender?.nickname ?: "시스템",
                type = notification.type?.name ?: "",
                rentId = notification.relatedId // rent ID 추가
            )
        }

        // 이미지 URL 포맷팅 - 디버깅 로그 추가
        private fun formatImageUrl(imageUrl: String?): String {
            println("️ formatImageUrl 호출 - 원본 URL: $imageUrl")

            if (imageUrl.isNullOrBlank()) {
                println(" 이미지 URL이 null 또는 빈 문자열")
                return "" // 빈 문자열로 반환 - 프론트엔드에서 placeholder 처리
            }

            val trimmedUrl = imageUrl.trim()
            val result = when {
                // 이미 완전한 URL인 경우 그대로 반환
                trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://") -> {
                    println(" 완전한 URL - 그대로 사용: $trimmedUrl")
                    trimmedUrl
                }

                trimmedUrl.startsWith("/") -> {
                    val url = "http://localhost:8080$trimmedUrl"
                    println(" 절대경로 변환: $url")
                    url
                }

                trimmedUrl.startsWith("uploads/") -> {
                    val url = "http://localhost:8080/$trimmedUrl"
                    println(" uploads 경로 변환: $url")
                    url
                }

                else -> {
                    val url = "http://localhost:8080/uploads/$trimmedUrl"
                    println(" 파일명만 있음 - uploads 폴더에서 찾기: $url")
                    url
                }
            }

            return result
        }

        // 시간 포맷팅 (3시간 전, 1일 전 등)
        private fun formatTime(createAt: LocalDateTime?): String {
            if (createAt == null) {
                return "알 수 없음"
            }

            val now = LocalDateTime.now()
            val hours = ChronoUnit.HOURS.between(createAt, now)
            val days = ChronoUnit.DAYS.between(createAt, now)

            return when {
                hours < 1 -> {
                    val minutes = ChronoUnit.MINUTES.between(createAt, now)
                    "${max(1, minutes.toInt())}분 전"
                }

                hours < 24 -> "${hours}시간 전"
                days < 7 -> "${days}일 전"
                else -> createAt.format(DateTimeFormatter.ofPattern("MM-dd"))
            }
        }
    }
}
