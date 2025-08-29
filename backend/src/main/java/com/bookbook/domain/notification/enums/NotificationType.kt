package com.bookbook.domain.notification.enums

//08-29 유효상
enum class NotificationType(val defaultTitle: String) {
    RENT_REQUEST("📘 도서 대여 요청이 도착했어요!"),
    RENT_APPROVED("✅ 대여 요청이 수락되었습니다"),
    RENT_REJECTED("❌ 대여 요청이 거절되었습니다"),
    WISHLIST_AVAILABLE("📕 찜한 도서가 대여 가능해졌습니다"),
    RETURN_REMINDER("📙 도서 반납일이 다가옵니다"),
    POST_CREATED("✅ 글이 등록되었습니다")
}
