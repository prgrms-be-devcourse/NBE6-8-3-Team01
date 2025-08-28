package com.bookbook.domain.rent.entity

// 대여 상태를 나타내는 enum 클래스
// 25.08.28 현준
enum class RentStatus(val description: String) {
    AVAILABLE("대여 가능"),    // 대여 가능 상태
    LOANED("대여 중"),        // 대여 중 상태
    FINISHED("대여 완료"),     // 대여 불가 상태
    DELETED("삭제 됨")        // 삭제된 상태
}