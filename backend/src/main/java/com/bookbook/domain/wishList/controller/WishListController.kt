package com.bookbook.domain.wishList.controller

import com.bookbook.domain.wishList.dto.WishListCreateRequestDto
import com.bookbook.domain.wishList.dto.WishListResponseDto
import com.bookbook.domain.wishList.service.WishListService
import com.bookbook.global.rsdata.RsData
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 찜 목록 관리 컨트롤러
 *
 * 사용자의 찜 목록 조회, 추가, 삭제 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/user/{userId}/wishlist")
class WishListController(
    private val wishListService: WishListService
) {
    /**
     * 찜 목록 조회 (검색 기능 포함)
     *
     * 사용자의 찜 목록을 생성일 역순으로 조회합니다.
     *
     * @param userId 사용자 ID
     * @param search 검색어 (선택사항, 책 제목/저자/출판사/게시글 제목에서 검색)
     * @return 찜 목록 리스트
     */
    @GetMapping
    fun getWishList(
        @PathVariable userId: Long,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<RsData<List<WishListResponseDto>?>> {
        val wishList = if (!search.isNullOrBlank()) {
            wishListService.searchWishListByUserId(userId, search)
        } else {
            wishListService.getWishListByUserId(userId)
        }
        return ResponseEntity.ok(RsData("200", "찜 목록을 조회했습니다.", wishList))
    }

    /**
     * 찜 목록에 도서 추가
     *
     * 사용자가 관심 있는 도서를 찜 목록에 추가합니다.
     * 이미 찜한 도서는 중복 추가할 수 없습니다.
     *
     * @param userId 사용자 ID
     * @param request 찜 추가 요청 정보 (도서 게시글 ID)
     * @return 생성된 찜 정보
     */
    @PostMapping
    fun addWishList(
        @PathVariable userId: Long,
        @RequestBody request: WishListCreateRequestDto
    ): ResponseEntity<RsData<WishListResponseDto>> {
        val response = wishListService.addWishList(userId, request)
        return ResponseEntity.ok(RsData("200", "찜 목록에 추가했습니다.", response))
    }

    /**
     * 찜 목록에서 도서 삭제
     *
     * 사용자의 찜 목록에서 특정 도서를 제거합니다.
     *
     * @param userId 사용자 ID
     * @param rentId 삭제할 도서 게시글 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{rentId}")
    fun deleteWishList(
        @PathVariable userId: Long,
        @PathVariable rentId: Long
    ): ResponseEntity<RsData<Unit>> {
        wishListService.deleteWishList(userId, rentId)
        return ResponseEntity.ok(RsData("200", "찜 목록에서 삭제했습니다.", Unit))
    }
}
