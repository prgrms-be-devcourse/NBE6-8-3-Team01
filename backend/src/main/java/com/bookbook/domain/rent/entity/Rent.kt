package com.bookbook.domain.rent.entity

import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

// 25.08.29 현준
// 대여 게시글을 나타내는 엔티티 클래스
@Entity
class Rent(
    // 대여자 정보
    @Column(nullable = false)
    var lenderUserId: Long, // 대여자 ID

    // 게시글 기본 정보
    @Column(nullable = false)
    var title: String, // 글 제목
    
    @Column(nullable = false)
    var bookCondition: String, // 책 상태
    
    @Column(nullable = false)
    var bookImage: String, // 글쓴이가 올린 책 이미지 URL
    
    @Column(nullable = false)
    var address: String, // 사용자 주소
    
    @Column(nullable = false, length = 500)
    var contents: String, // 대여 내용

    // 대여 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var rentStatus: RentStatus, // 대여 상태(Available, Loaned, Finished, Deleted)

    // 알라딘 API 도서 정보
    @Column(nullable = false)
    var bookTitle: String, // 책 제목
    
    @Column(nullable = false)
    var author: String, // 책 저자
    
    @Column(nullable = false)
    var publisher: String, // 책 출판사
    
    @Column(nullable = false)
    var category: String, // 책 카테고리
    
    @Column(nullable = false, length = 500)
    var description: String // 책 설명
) : BaseEntity()