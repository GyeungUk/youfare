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
    // category는 enum 그대로 바인딩하면 PostgreSQL이 ":category IS NULL"의 파라미터 타입을
    // 추론하지 못해(could not determine data type of parameter) 쿼리 준비 단계에서 실패한다.
    // → enum.name() 문자열로 받아 CAST(w.category AS string)과 비교해 타입을 명확히 한다.
    // status 필터: 신청 시작일/마감일과 오늘(:today)을 비교해 진행중/마감/예정을 구분한다.
    //   ONGOING  = 마감 안 됨(상시모집 NULL 포함) + 시작됨(상시모집 NULL 포함)
    //   CLOSED   = 마감일이 오늘 이전
    //   UPCOMING = 시작일이 오늘 이후
    // category와 동일하게 enum 대신 String으로 받아 ":status IS NULL" 타입 추론 오류를 피한다.
    @Query("SELECT w FROM Welfare w WHERE " +
           "(:category IS NULL OR CAST(w.category AS string) = :category) AND " +
           "(:region IS NULL OR w.region = '전국' OR w.region LIKE CONCAT('%', CAST(:region AS string), '%')) AND " +
           "(:status IS NULL OR (" +
           "  (:status = 'ONGOING'  AND (w.applyStartDate IS NULL OR w.applyStartDate <= :today) " +
           "                        AND (w.applyEndDate   IS NULL OR w.applyEndDate   >= :today)) OR " +
           "  (:status = 'CLOSED'   AND w.applyEndDate   IS NOT NULL AND w.applyEndDate   < :today) OR " +
           "  (:status = 'UPCOMING' AND w.applyStartDate IS NOT NULL AND w.applyStartDate > :today)))")
    Page<Welfare> findByFilter(
            @Param("category") String category,
            @Param("region") String region,
            @Param("status") String status,
            @Param("today") LocalDate today,
            Pageable pageable);

    // 개인화 추천: 나이 + 지역 + 신청 마감일 기준 필터
    // 마감일 조건: applyEndDate가 NULL(상시모집)이거나 오늘 이후인 건을 모두 포함
    //   (NULL을 제외하면 상시 사업이 추천에서 통째로 빠지므로 명시적으로 OR NULL 처리)
    // 나이 조건 괄호: (:age IS NULL OR (minAge조건 AND maxAge조건)) 명시적으로 래핑
    // category 필터: findByFilter와 동일하게 enum.name() 문자열로 받아 타입 추론 오류 회피
    // 정렬: 내 지역 사업 먼저(서울 유저면 서울 우선) → 전국은 그 뒤 → 마감 임박순 → 최신 등록순
    //   (WHERE가 지역을 {내 지역, 전국}으로 좁히므로 '전국'이 아닌 건 곧 내 지역 사업이다.
    //    서울 거주자는 서울 혜택부터 보고 싶어 하므로 전국 더미를 뒤로 미룬다.)
    @Query("SELECT w FROM Welfare w WHERE " +
           "(w.applyEndDate IS NULL OR w.applyEndDate >= :today) AND " +
           "(:category IS NULL OR CAST(w.category AS string) = :category) AND " +
           "(:age IS NULL OR ((w.targetAgeMin IS NULL OR w.targetAgeMin <= :age) AND " +
           "                  (w.targetAgeMax IS NULL OR w.targetAgeMax >= :age))) AND " +
           "(:region IS NULL OR w.region = '전국' OR w.region LIKE CONCAT('%', CAST(:region AS string), '%')) " +
           "ORDER BY CASE WHEN w.region = '전국' THEN 1 ELSE 0 END ASC, " +
           "         w.applyEndDate ASC NULLS LAST, w.id DESC")
    Page<Welfare> findPersonalized(
            @Param("age") Integer age,
            @Param("region") String region,
            @Param("category") String category,
            @Param("today") LocalDate today,
            Pageable pageable);
}
