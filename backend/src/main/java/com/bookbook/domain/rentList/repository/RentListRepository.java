package com.bookbook.domain.rentList.repository;
//08-06 유효상
import com.bookbook.domain.rentList.entity.RentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentListRepository extends JpaRepository<RentList, Long> {
    
    List<RentList> findByBorrowerUserIdOrderByCreatedDateDesc(Long borrowerUserId);
    
    List<RentList> findByBorrowerUserIdAndStatus(Long borrowerUserId, RentRequestStatus status);

    List<RentList> findByRentId(Long rentId);

    Optional<RentList> findByBorrowerUserIdAndRentId(Long borrowerUserId, Long rentId);

    List<RentList> findByRentIdAndStatus(Long rentId, RentRequestStatus status);

    // 특정 신청자의 특정 책에 대한 특정 상태의 신청 조회
    List<RentList> findByRentIdAndBorrowerUserIdAndStatus(Long rentId, Long borrowerUserId, RentRequestStatus status);

    // 특정 신청자의 특정 책에 대한 모든 신청 조회 (상태 무관)
    List<RentList> findByRentIdAndBorrowerUserId(Long rentId, Long borrowerUserId);

    // 중복 신청 방지를 위한 메서드
    boolean existsByBorrowerUserIdAndRentIdAndStatus(Long borrowerUserId, Long rentId, RentRequestStatus status);
}