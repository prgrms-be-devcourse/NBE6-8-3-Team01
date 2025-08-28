package com.bookbook.domain.rent.entity

import com.bookbook.global.jpa.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import lombok.*

// 게시글을 나타내는 엔티티 클래스
// 25.08.28 현준
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Rent : BaseEntity() {
    // 빌려주는 유저 ID
    private var lenderUserId: Long? = null // 대여자 ID

    // 글 정보
    private var title: String? = null // 글 제목
    private var bookCondition: String? = null // 책 상태
    private var bookImage: String? = null // 글쓴이가 올린 책 이미지 URL
    private var address: String? = null // 사용자 주소
    private var contents: String? = null // 대여 내용

    // 대여 글 상태
    @Enumerated(EnumType.STRING) // enum 이름을 String으로 DB에 저장하도록 지정
    private var rentStatus: RentStatus? = null // 대여 상태(Available, Loaned, Finished, Deleted)

    // 알라딘 API로 받아온 책 관련 속성
    private var bookTitle: String? = null // 책 제목
    private var author: String? = null // 책 저자
    private var publisher: String? = null // 책 출판사
    private var category: String? = null // 책 카테고리
    private var description: String? = null // 책 설명
}