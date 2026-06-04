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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 온통청년 청년정책 데이터를 Welfare 엔티티로 정규화·upsert하는 서비스.
 *
 * [트랜잭션 전략]
 * 외부 API 호출 동안 커넥션 점유를 막기 위해 오케스트레이터(PublicDataSyncService)는
 * 트랜잭션 없이 동작하고, 단건 저장만 REQUIRES_NEW 독립 트랜잭션으로 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WelfareUpsertService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WelfareRepository welfareRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertOne(PublicWelfareItem item) {
        if (!StringUtils.hasText(item.getPlcyNo()) || !StringUtils.hasText(item.getPlcyNm())) return;

        WelfareCategory category = mapCategory(item.getLclsfNm());
        LocalDate startDate = parseDate(item.getAplyYmd(), 0);
        LocalDate endDate = parseDate(item.getAplyYmd(), 1);
        Integer ageMin = (item.getSprtTrgtMinAge() != null && item.getSprtTrgtMinAge() > 0)
                ? item.getSprtTrgtMinAge() : 19;
        Integer ageMax = (item.getSprtTrgtMaxAge() != null && item.getSprtTrgtMaxAge() > 0)
                ? item.getSprtTrgtMaxAge() : 34;
        String incomeCondition = buildIncomeCondition(item);
        String sourceUrl = StringUtils.hasText(item.getAplyUrlAddr())
                ? item.getAplyUrlAddr() : item.getRefUrlAddr1();
        String region = normalizeRegion(item.getSprvsnInstCdNm());

        welfareRepository.findByExternalId(item.getPlcyNo())
                .ifPresentOrElse(
                        existing -> existing.update(
                                item.getPlcyNm(), category, item.getPlcyExplnCn(),
                                ageMin, ageMax, region,
                                incomeCondition, startDate, endDate,
                                sourceUrl
                        ),
                        () -> welfareRepository.save(Welfare.builder()
                                .externalId(item.getPlcyNo())
                                .title(item.getPlcyNm())
                                .category(category)
                                .description(item.getPlcyExplnCn())
                                .targetAgeMin(ageMin)
                                .targetAgeMax(ageMax)
                                .region(region)
                                .incomeCondition(incomeCondition)
                                .applyStartDate(startDate)
                                .applyEndDate(endDate)
                                .sourceUrl(sourceUrl)
                                .build())
                );
    }

    /**
     * 화면에 바로 보여줄 수 있는 "구체적인" 소득 조건 문구를 만든다.
     *
     * 온통청년 API의 소득 조건은 세 갈래로 들어온다.
     *  - earnEtcCn(기타 설명): "월 소득 154만원 이하(중위소득 60%)" 등 가장 구체적 → 그대로 사용
     *  - earnCndSeCd=0043002(연소득): 금액은 earnMinAmt/earnMaxAmt(만원 단위)에 들어옴 → "연 소득 N만원 이하"로 조립
     *  - earnCndSeCd=0043001(무관): "제한 없음"
     * 예전에는 0043002를 "소득 조건 있음"이라고만 노출해, 정작 중요한 금액 기준이 사라졌었다.
     */
    private String buildIncomeCondition(PublicWelfareItem item) {
        // 1) 기타 상세 설명이 있으면 가장 구체적이므로 최우선
        if (StringUtils.hasText(item.getEarnEtcCn())) {
            return item.getEarnEtcCn().trim();
        }

        long min = parseAmount(item.getEarnMinAmt());
        long max = parseAmount(item.getEarnMaxAmt());
        String code = item.getEarnCndSeCd() == null ? "" : item.getEarnCndSeCd().trim();

        // 2) 연소득 기준: 실제 금액(만원)을 사람이 읽을 문장으로
        String range = formatIncomeRange(min, max);
        if (range != null) return range;

        // 3) 명시적으로 무관인 경우
        if ("0043001".equals(code)) return "제한 없음";

        // 4) 금액도 설명도 없는 기타: 최소한 "기준이 있다"는 사실은 알린다
        if ("0043002".equals(code) || "0043003".equals(code)) return "별도 소득 기준 있음";

        return "제한 없음";
    }

    /** "3692" 같은 만원 단위 금액 문자열 → long. 숫자가 아니거나 비면 0. */
    private long parseAmount(String raw) {
        if (!StringUtils.hasText(raw)) return 0;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 만원 단위 최소/최대 금액 → "연 소득 N만원 이하/이상/범위" 문구. 둘 다 0이면 null. */
    private String formatIncomeRange(long min, long max) {
        if (min > 0 && max > 0) {
            return String.format("연 소득 %,d만원 ~ %,d만원", min, max);
        }
        if (max > 0) return String.format("연 소득 %,d만원 이하", max);
        if (min > 0) return String.format("연 소득 %,d만원 이상", min);
        return null;
    }

    /**
     * 주관기관명(sprvsnInstCdNm) → 온보딩과 동일한 시·도 표기로 정규화.
     *
     * 공공데이터의 주관기관명은 "충청북도", "광주시청", "고용노동부", "청년정책과"처럼
     * 형태가 제각각이라 그대로 두면 지역 필터/추천이 거의 동작하지 않는다.
     * - 시·도 키워드(정식명·약칭 모두)를 포함하면 해당 시·도 약칭으로 매핑
     * - 중앙부처 등 특정 시·도로 볼 수 없는 기관은 "전국"으로 처리해 모든 유저에게 노출
     */
    private static final Map<String, String> REGION_KEYWORDS = buildRegionKeywords();

    private static Map<String, String> buildRegionKeywords() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("서울", "서울");
        m.put("부산", "부산");
        m.put("인천", "인천");
        m.put("대구", "대구");
        m.put("대전", "대전");
        m.put("광주", "광주");
        m.put("울산", "울산");
        m.put("세종", "세종");
        m.put("경기", "경기");
        m.put("강원", "강원");
        m.put("충청북", "충북");
        m.put("충북", "충북");
        m.put("충청남", "충남");
        m.put("충남", "충남");
        m.put("전라북", "전북");
        m.put("전북", "전북");
        m.put("전라남", "전남");
        m.put("전남", "전남");
        m.put("경상북", "경북");
        m.put("경북", "경북");
        m.put("경상남", "경남");
        m.put("경남", "경남");
        m.put("제주", "제주");
        return m;
    }

    private String normalizeRegion(String instName) {
        if (!StringUtils.hasText(instName)) return "전국";
        for (Map.Entry<String, String> e : REGION_KEYWORDS.entrySet()) {
            if (instName.contains(e.getKey())) return e.getValue();
        }
        return "전국";
    }

    /**
     * 온통청년 정책 대분류명(lclsfNm) → WelfareCategory
     * 일자리 / 주거 / 교육･직업훈련 / 금융･복지･문화 / 참여･기반
     */
    private WelfareCategory mapCategory(String lclsfNm) {
        if (!StringUtils.hasText(lclsfNm)) return WelfareCategory.ETC;
        if (lclsfNm.contains("일자리")) return WelfareCategory.EMPLOYMENT;
        if (lclsfNm.contains("주거")) return WelfareCategory.HOUSING;
        if (lclsfNm.contains("교육")) return WelfareCategory.EDUCATION;
        if (lclsfNm.contains("금융") || lclsfNm.contains("복지") || lclsfNm.contains("문화")) {
            return WelfareCategory.FINANCE;
        }
        return WelfareCategory.ETC;
    }

    /**
     * "yyyyMMdd ~ yyyyMMdd" 형식의 신청 기간 문자열에서 시작일(idx=0)/종료일(idx=1)을 파싱.
     * 값이 비어 있거나(상시 등) 파싱 불가하면 null.
     */
    private LocalDate parseDate(String aplyYmd, int idx) {
        if (!StringUtils.hasText(aplyYmd) || !aplyYmd.contains("~")) return null;
        String[] parts = aplyYmd.split("~");
        if (idx >= parts.length) return null;
        try {
            return LocalDate.parse(parts[idx].trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
