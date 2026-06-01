package com.youfare.domain.scrap;

import com.youfare.domain.user.User;
import com.youfare.domain.welfare.Welfare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    boolean existsByUserAndWelfare(User user, Welfare welfare);

    Optional<Scrap> findByUserAndWelfare(User user, Welfare welfare);

    /**
     * 내 스크랩 목록 조회
     * JOIN FETCH로 welfare를 함께 로딩 → N+1 쿼리 방지
     * (welfare는 LAZY이므로 fetch 없이 조회하면 스크랩 N개당 추가 쿼리 N번 발생)
     */
    @Query("SELECT s FROM Scrap s JOIN FETCH s.welfare WHERE s.user = :user ORDER BY s.createdAt DESC")
    List<Scrap> findAllByUserWithWelfare(@Param("user") User user);
}
