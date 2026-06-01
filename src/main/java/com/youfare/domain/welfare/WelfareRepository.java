package com.youfare.domain.welfare;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface WelfareRepository extends JpaRepository<Welfare, Long> {

    Optional<Welfare> findByExternalId(String externalId);

    // 카테고리 + 지역 필터 페이징 조회
    // LIKE %:region% 는 일부 JPA 구현체에서 파싱 오류 → CONCAT 방식으로 안전하게 처리
    @Query("SELECT w FROM Welfare w WHERE " +
           "(:category IS NULL OR w.category = :category) AND " +
           "(:region IS NULL OR w.region = '전국' OR w.region LIKE CONCAT('%', :region, '%'))")
    Page<Welfare> findByFilter(
            @Param("category") WelfareCategory category,
            @Param("region") String region,
            Pageable pageable);

    // 개인화 추천: 나이 + 지역 + 신청 마감일 기준 필터
    // 나이 조건 괄호: (:age IS NULL OR (minAge조건 AND maxAge조건)) 명시적으로 래핑
    @Query("SELECT w FROM Welfare w WHERE " +
           "w.applyEndDate >= :today AND " +
           "(:age IS NULL OR ((w.targetAgeMin IS NULL OR w.targetAgeMin <= :age) AND " +
           "                  (w.targetAgeMax IS NULL OR w.targetAgeMax >= :age))) AND " +
           "(:region IS NULL OR w.region = '전국' OR w.region LIKE CONCAT('%', :region, '%'))")
    Page<Welfare> findPersonalized(
            @Param("age") Integer age,
            @Param("region") String region,
            @Param("today") LocalDate today,
            Pageable pageable);
}
