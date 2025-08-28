package com.bookbook.domain.lendList.service;

import com.bookbook.domain.lendList.dto.LendListResponseDto;
import com.bookbook.domain.rent.entity.Rent;
import com.bookbook.domain.rent.entity.RentStatus;
import com.bookbook.domain.rent.repository.RentRepository;
import com.bookbook.domain.rentList.entity.RentList;
import com.bookbook.domain.rentList.repository.RentListRepository;
import com.bookbook.domain.review.repository.ReviewRepository;
import com.bookbook.domain.user.entity.User;
import com.bookbook.domain.user.repository.UserRepository;
import com.bookbook.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LendListService {

    private final RentRepository rentRepository;
    private final RentListRepository rentListRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 사용자가 등록한 도서 목록 조회
     */
    public Page<LendListResponseDto> getLendListByUserId(Long userId, Pageable pageable) {
        // 기존의 findFilteredRentHistory 메소드 사용
        Page<Rent> rentPage = rentRepository.findFilteredRentHistory(pageable, 
                Arrays.asList(RentStatus.AVAILABLE, RentStatus.LOANED, RentStatus.FINISHED), userId);
        
        return rentPage.map(rent -> {
            String borrowerNickname = "대여 신청 없음";
            boolean hasReview = false;

            // 대여 신청이 있는지 확인
            List<RentList> rentLists = rentListRepository.findByRentId(rent.getId());
            RentList rentList = rentLists.isEmpty() ? null : rentLists.get(0);

            if (rentList != null) {
                // 대여자 닉네임 조회
                borrowerNickname = userRepository.findById(rentList.getBorrowerUser().getId())
                        .map(User::getNickname)
                        .orElse("알 수 없는 사용자");

                // 리뷰 작성 여부 확인
                hasReview = reviewRepository.findByRentIdAndReviewerId(rent.getId(), userId)
                        .isPresent();
            }

            return LendListResponseDto.from(rent, borrowerNickname, 
                    rentList != null ? rentList.getReturnDate() : null, hasReview);
        });
    }

    /**
     * 사용자가 등록한 도서 목록 조회 (검색 기능 포함)
     */
    public Page<LendListResponseDto> getLendListByUserIdAndSearch(Long userId, String search, Pageable pageable) {
        // 사용자가 등록한 모든 게시글을 가져온 후 검색어로 필터링
        Page<Rent> rentPage = rentRepository.findFilteredRentHistory(pageable, 
                Arrays.asList(RentStatus.AVAILABLE, RentStatus.LOANED, RentStatus.FINISHED), userId);
        
        return rentPage.map(rent -> {
            String borrowerNickname = "대여 신청 없음";
            boolean hasReview = false;

            // 대여 신청이 있는지 확인
            List<RentList> rentLists = rentListRepository.findByRentId(rent.getId());
            RentList rentList = rentLists.isEmpty() ? null : rentLists.get(0);

            if (rentList != null) {
                // 대여자 닉네임 조회
                borrowerNickname = userRepository.findById(rentList.getBorrowerUser().getId())
                        .map(User::getNickname)
                        .orElse("알 수 없는 사용자");

                // 리뷰 작성 여부 확인
                hasReview = reviewRepository.findByRentIdAndReviewerId(rent.getId(), userId)
                        .isPresent();
            }

            return LendListResponseDto.from(rent, borrowerNickname, 
                    rentList != null ? rentList.getReturnDate() : null, hasReview);
        });
    }

    /**
     * 완료된 도서 대여 목록 조회
     */
    public Page<LendListResponseDto> getCompletedLendList(Long userId, Pageable pageable) {
        Page<Rent> rentPage = rentRepository.findFilteredRentHistory(pageable, 
                Arrays.asList(RentStatus.FINISHED), userId);
        
        return rentPage.map(rent -> {
            String borrowerNickname = "대여 신청 없음";
            boolean hasReview = false;

            // 대여 신청이 있는지 확인
            List<RentList> rentLists = rentListRepository.findByRentId(rent.getId());
            RentList rentList = rentLists.isEmpty() ? null : rentLists.get(0);

            if (rentList != null) {
                // 대여자 닉네임 조회
                borrowerNickname = userRepository.findById(rentList.getBorrowerUser().getId())
                        .map(User::getNickname)
                        .orElse("알 수 없는 사용자");

                // 리뷰 작성 여부 확인
                hasReview = reviewRepository.findByRentIdAndReviewerId(rent.getId(), userId)
                        .isPresent();
            }

            return LendListResponseDto.from(rent, borrowerNickname, 
                    rentList != null ? rentList.getReturnDate() : null, hasReview);
        });
    }

    /**
     * 도서 게시글 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteLendList(Long userId, Long rentId) {
        Rent rent = rentRepository.findById(rentId)
                .orElseThrow(() -> new ServiceException("404-RENT-NOT-FOUND", "해당 대여 게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!rent.getLenderUserId().equals(userId)) {
            throw new ServiceException("403-FORBIDDEN", "해당 게시글을 삭제할 권한이 없습니다.");
        }

        // 소프트 삭제 (상태를 DELETED로 변경)
        rent.setRentStatus(RentStatus.DELETED);
        rentRepository.save(rent);
    }
}