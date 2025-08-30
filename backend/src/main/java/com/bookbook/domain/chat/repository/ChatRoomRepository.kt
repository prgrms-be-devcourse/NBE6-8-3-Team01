package com.bookbook.domain.chat.repository

import com.bookbook.domain.chat.entity.ChatRoom
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ChatRoomRepository : JpaRepository<ChatRoom, Long> {
    // 사용자별 채팅방 목록 조회 (최신 메시지 순) - 메시지가 있는 채팅방만
    @Query(
        """SELECT cr FROM ChatRoom cr 
           WHERE (cr.lenderId = :userId OR cr.borrowerId = :userId) 
           AND cr.isActive = true 
           AND EXISTS (SELECT 1 FROM ChatMessage cm WHERE cm.roomId = cr.roomId) 
           ORDER BY cr.lastMessageTime DESC NULLS LAST, cr.createdDate DESC"""
    )
    fun findByUserIdOrderByLastMessageTimeDesc(@Param("userId") userId: Long, pageable: Pageable): Page<ChatRoom>

    // 특정 대여 게시글에 대한 채팅방 존재 여부 확인
    fun findByRentIdAndLenderIdAndBorrowerId(rentId: Long, lenderId: Long, borrowerId: Long): Optional<ChatRoom>

    // 특정 대여 게시글에 대한 활성 채팅방들을 생성일 기준 내림차순으로 조회 (중복 방지)
    @Query("""SELECT cr FROM ChatRoom cr 
              WHERE cr.rentId = :rentId 
              AND cr.lenderId = :lenderId 
              AND cr.borrowerId = :borrowerId 
              AND cr.isActive = true 
              ORDER BY cr.createdDate DESC""")
    fun findByRentIdAndLenderIdAndBorrowerIdOrderByCreatedDateDesc(
        @Param("rentId") rentId: Long,
        @Param("lenderId") lenderId: Long,
        @Param("borrowerId") borrowerId: Long
    ): List<ChatRoom>

    // 중복 채팅방 정리를 위한 삭제 메서드
    @Modifying
    @Query("DELETE FROM ChatRoom cr WHERE cr.id = :chatRoomId")
    fun deleteByIdCustom(@Param("chatRoomId") chatRoomId: Long): Int

    // 채팅방 ID로 조회
    fun findByRoomId(roomId: String): Optional<ChatRoom>

    // 사용자가 참여한 채팅방인지 확인 (나간 사용자도 포함)
    @Query(
        """SELECT cr FROM ChatRoom cr 
           WHERE cr.roomId = :roomId 
           AND (cr.lenderId = :userId OR cr.borrowerId = :userId) 
           AND cr.isActive = true"""
    )
    fun findByRoomIdAndUserId(@Param("roomId") roomId: String, @Param("userId") userId: Long): Optional<ChatRoom>

    // 사용자의 활성 채팅방 개수
    @Query(
        """SELECT COUNT(cr) FROM ChatRoom cr 
           WHERE (cr.lenderId = :userId OR cr.borrowerId = :userId) 
           AND cr.isActive = true"""
    )
    fun countActiveRoomsByUserId(@Param("userId") userId: Long): Long

    // 사용자가 참여한 모든 채팅방 조회
    fun findByLenderIdOrBorrowerId(lenderId: Long, borrowerId: Long): List<ChatRoom>
}