package com.bookbook.domain.chat.entity

import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "chat_room")
class ChatRoom : BaseEntity() {
    @Column(unique = true, nullable = false)
    var roomId: String = ""

    @Column(nullable = false)
    var rentId: Long = 0L

    @Column(nullable = false)
    var lenderId: Long = 0L

    @Column(nullable = false)
    var borrowerId: Long = 0L

    @Column(nullable = false)
    var isActive: Boolean = true

    @Column(columnDefinition = "TEXT")
    var leftUserIds: String? = null

    @Column(columnDefinition = "TEXT")
    var userLeftTimes: String? = null

    var lastMessageTime: LocalDateTime? = null

    var lastMessage: String? = null

    // 채팅방이 특정 사용자에게 속하는지 확인
    fun belongsToUser(userId: Long?): Boolean {
        return lenderId == userId || borrowerId == userId
    }

    // 상대방 ID 가져오기
    fun getOtherUserId(currentUserId: Long?): Long? {
        return if (lenderId == currentUserId) borrowerId else lenderId
    }

    // 마지막 메시지 업데이트
    fun updateLastMessage(message: String?, messageTime: LocalDateTime?) {
        this.lastMessage = message
        this.lastMessageTime = messageTime
    }

    // 사용자가 채팅방을 나간 것으로 표시
    fun markUserAsLeft(userId: Long) {
        val leftTime = LocalDateTime.now()

        // leftUserIds 업데이트
        if (leftUserIds.isNullOrEmpty()) {
            leftUserIds = userId.toString()
        } else if (!leftUserIds!!.contains(userId.toString())) {
            leftUserIds += ",$userId"
        }

        // 나간 시간 기록
        val timeRecord = "$userId:$leftTime"
        if (userLeftTimes.isNullOrEmpty()) {
            userLeftTimes = timeRecord
        } else {
            // 기존 기록 제거 후 새로 추가
            userLeftTimes = removeUserLeftTime(userId) + ",$timeRecord"
            userLeftTimes = userLeftTimes!!.replace("^,|,$".toRegex(), "") // 앞뒤 쉼표 제거
        }
    }

    // 사용자가 나간 상태인지 확인
    fun hasUserLeft(userId: Long): Boolean {
        if (leftUserIds.isNullOrEmpty()) {
            return false
        }

        val userIdStr = userId.toString()
        val leftIds = leftUserIds!!.split(",")

        return leftIds.any { it.trim() == userIdStr }
    }

    // 사용자가 나간 시간 가져오기
    fun getUserLeftTime(userId: Long): LocalDateTime? {
        if (userLeftTimes.isNullOrEmpty()) {
            return null
        }

        val records = userLeftTimes!!.split(",")
        for (record in records) {
            val parts = record.split(":")
            if (parts.size >= 2 && parts[0] == userId.toString()) {
                try {
                    // userId:yyyy-MM-ddTHH:mm:ss.nnnnnnnnn 형식에서 시간 부분 추출
                    val timeStr = record.substring(parts[0].length + 1)
                    return LocalDateTime.parse(timeStr)
                } catch (e: Exception) {
                    return null
                }
            }
        }
        return null
    }

    // 특정 사용자의 나간 시간 기록 제거
    private fun removeUserLeftTime(userId: Long): String {
        if (userLeftTimes.isNullOrEmpty()) {
            return ""
        }

        val records = userLeftTimes!!.split(",")
        return records.filter { !it.startsWith("$userId:") }.joinToString(",")
    }

    // 사용자가 채팅방에 다시 들어올 때 나간 상태 해제
    fun rejoinUser(userId: Long) {
        if (hasUserLeft(userId)) {
            val userIdStr = userId.toString()
            val leftIds = leftUserIds!!.split(",")
            val newLeftIds = leftIds.filter { it.trim() != userIdStr }.joinToString(",")
            
            leftUserIds = if (newLeftIds.isNotEmpty()) newLeftIds else null

            // 나간 시간 기록도 제거
            val removedTime = removeUserLeftTime(userId)
            userLeftTimes = if (removedTime.isEmpty()) null else removedTime
        }
    }

    // 채팅방의 모든 사용자 나간 상태를 안전하게 초기화 (재활성화)
    fun resetUserLeftStatus() {
        this.leftUserIds = null
        this.userLeftTimes = null
        this.isActive = true
    }

    // 채팅방이 완전히 비어있는지 확인 (모든 사용자가 나갔는지)
    val isEmpty: Boolean
        get() = hasUserLeft(lenderId) && hasUserLeft(borrowerId)

    // 채팅방에 활성 사용자가 있는지 확인
    fun hasActiveUsers(): Boolean {
        return !hasUserLeft(lenderId) || !hasUserLeft(borrowerId)
    }
}