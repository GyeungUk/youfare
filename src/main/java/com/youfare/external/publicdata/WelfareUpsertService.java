package com.youfare.external.publicdata;

import com.youfare.domain.welfare.Welfare;
import com.youfare.domain.welfare.WelfareCategory;
import com.youfare.domain.welfare.WelfareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 외부 복지 데이터를 Welfare 엔티티로 정규화·upsert하는 서비스.
 *
 * [최적화 포인트 — 트랜잭션 범위 분리]
 * 전체 동기화를 하나의 트랜잭션으로 묶으면:
 *  - 느린 외부 API 호출 동안 DB 커넥션을 길게 점유
 *  - 단건 실패로 영속성 컨텍스트가 오염되면 전체 롤백
 * 따라서 "페이지 단위"로 트랜잭션을 끊는다.
 * 단건은 REQUIRES_NEW로 독립 트랜잭션을 부여해, 한 건 실패가 같은 페이지의
 * 다른 건에 영향을 주지 않도록 격리한다(부분 성공 허용).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WelfareUpsertService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WelfareRepository welfareRepository;

    /** 한 건을 독립 트랜잭션으로 upsert. 실패해도 다른 건에 영향 없음. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertOne(PublicWelfareItem item) {
        if (!StringUtils.hasText(item.getServId())) return;

        WelfareCategory category = mapCategory(item.getLifeNmArray());
        LocalDate startDate = parseDate(item.getAplyYmd());
        LocalDate endDate = parseDate(item.getAplyEndYmd());
        String region = normalizeRegion(item.getRprsCtadr());

        welfareRepository.findByExternalId(item.getServId())
                .ifPresentOrElse(
                        existing -> existing.update(
                                item.getServNm(), category, item.getServDgst(),
                                19, 34, region,
                                item.getAlwServCn(), startDate, endDate,
                                item.getInqplCtadrUrl()
                        ),
                        () -> welfareRepository.save(Welfare.builder()
                                .externalId(item.getServId())
                                .title(item.getServNm())
                                .category(category)
                                .description(item.getServDgst())
                                .targetAgeMin(19)
                                .targetAgeMax(34)
                                .region(region)
                                .incomeCondition(item.getAlwServCn())
                                .applyStartDate(startDate)
                                .applyEndDate(endDate)
                                .sourceUrl(item.getInqplCtadrUrl())
                                .build())
                );
    }

    /**
     * 원본 분류명 → WelfareCategory 매핑
     * 공공데이터 lifeNmArray 예: "취업/창업", "주거", "금융", "교육" 등
     */
    private WelfareCategory mapCategory(String lifeNm) {
        if (!StringUtils.hasText(lifeNm)) return WelfareCategory.ETC;
        if (lifeNm.contains("주거")) return WelfareCategory.HOUSING;
        if (lifeNm.contains("취업") || lifeNm.contains("창업") || lifeNm.contains("고용"))
            return WelfareCategory.EMPLOYMENT;
        if (lifeNm.contains("금융") || lifeNm.contains("대출") || lifeNm.contains("적금"))
            return WelfareCategory.FINANCE;
        if (lifeNm.contains("교육") || lifeNm.contains("장학")) return WelfareCategory.EDUCATION;
        return WelfareCategory.ETC;
    }

    private LocalDate parseDate(String yyyyMMdd) {
        if (!StringUtils.hasText(yyyyMMdd)) return null;
        try {
            return LocalDate.parse(yyyyMMdd.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String normalizeRegion(String rprsCtadr) {
        if (!StringUtils.hasText(rprsCtadr)) return "전국";
        return rprsCtadr.trim();
    }
}
