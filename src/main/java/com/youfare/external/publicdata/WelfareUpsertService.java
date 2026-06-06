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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String region = resolveRegion(sourceUrl, item.getSprvsnInstCdNm(),
                item.getPlcyNm(), item.getPlcyExplnCn());

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

    /**
     * 지역 판정 우선순위: source_url 호스트 → 주관기관명 → 제목(시·도 → 시·군) → 전국.
     *
     * 온통청년 데이터는 시·도별 청년포털(youth.chungnam.go.kr 등)에서 모은 것이라
     * "주관기관명"이 중앙부처거나 대학이면 실제론 지역사업인데도 전국으로 새는 문제가 있었다.
     * (예: SW중심대학·광역이음(충청권 연계) → 기관명만 보면 전국, 출처는 충남 포털)
     * 그래서 ① 출처 URL 호스트의 지역 토큰을 가장 신뢰도 높은 신호로 먼저 보고,
     * ② 기관명, ③ 제목에 박힌 시·도/시·군 이름까지 차례로 확인해 지역사업을 걸러낸다.
     * (예: "충청남도 전국청년축제", "천안 유니브시티" → 제목으로 충남 판정)
     */
    private String resolveRegion(String sourceUrl, String instName, String title, String description) {
        String byHost = regionFromHost(sourceUrl);
        if (byHost != null) return byHost;
        String byInst = matchKeyword(instName, REGION_KEYWORDS);
        if (byInst != null) return byInst;
        String byTitleSido = matchKeyword(title, REGION_KEYWORDS);
        if (byTitleSido != null) return byTitleSido;
        String byTitleCity = matchKeyword(title, CITY_KEYWORDS);
        if (byTitleCity != null) return byTitleCity;
        // 제목·기관·URL로 못 잡으면 본문(설명)까지 본다.
        // 지역사업은 보통 "○○시 관내·거주" 식으로 설명에 소재지를 명시한다(예: 서귀포YWCA 청년 일자리).
        String byDescSido = matchKeyword(description, REGION_KEYWORDS);
        if (byDescSido != null) return byDescSido;
        String byDescCity = matchKeyword(description, CITY_KEYWORDS);
        if (byDescCity != null) return byDescCity;
        return "전국";
    }

    /** text에 map의 키가 포함되면 해당 시·도 약칭 반환(등록 순서대로 첫 매치). 없으면 null. */
    private String matchKeyword(String text, Map<String, String> map) {
        if (!StringUtils.hasText(text)) return null;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (text.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    // 제목에 등장하는 주요 시·군명(한글) → 상위 시·도. DB 백필 로직과 동일하게 유지할 것.
    private static final Map<String, String> CITY_KEYWORDS = buildCityKeywords();

    private static Map<String, String> buildCityKeywords() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("춘천", "강원"); m.put("원주", "강원"); m.put("강릉", "강원");
        m.put("천안", "충남"); m.put("아산", "충남"); m.put("공주", "충남");
        m.put("홍성", "충남"); m.put("당진", "충남"); m.put("태안", "충남");
        m.put("계룡", "충남"); m.put("보령", "충남"); m.put("서산", "충남"); m.put("논산", "충남");
        m.put("충주", "충북"); m.put("제천", "충북");
        m.put("창원", "경남"); m.put("통영", "경남"); m.put("김해", "경남");
        m.put("진주", "경남"); m.put("양산", "경남"); m.put("거제", "경남");
        m.put("구미", "경북"); m.put("포항", "경북"); m.put("경주", "경북"); m.put("안동", "경북");
        m.put("순천", "전남"); m.put("해남", "전남"); m.put("장흥", "전남");
        m.put("여수", "전남"); m.put("목포", "전남");
        m.put("영암", "전남"); m.put("강진", "전남"); m.put("곡성", "전남"); m.put("완도", "전남");
        m.put("익산", "전북"); m.put("전주", "전북"); m.put("군산", "전북");
        m.put("순창", "전북"); m.put("장수", "전북");
        m.put("시흥", "경기"); m.put("의왕", "경기"); m.put("김포", "경기");
        m.put("수원", "경기"); m.put("성남", "경기"); m.put("고양", "경기"); m.put("용인", "경기");
        m.put("부천", "경기"); m.put("광명", "경기"); m.put("화성", "경기");
        m.put("음성", "충북"); m.put("청송", "경북");
        m.put("의령", "경남"); m.put("사천", "경남");
        m.put("화천", "강원"); m.put("홍천", "강원"); m.put("속초", "강원");
        // 구(區) 단위 — 동일 이름이 여러 광역시에 없는 유일 지명만 (서구·북구·중구 등 모호한 건 제외)
        m.put("남동구", "인천"); m.put("계양구", "인천");
        m.put("사상구", "부산"); m.put("영도구", "부산"); m.put("연제구", "부산");
        m.put("강동구", "서울");
        m.put("서귀포", "제주");
        m.put("기장", "부산");
        return m;
    }

    /** source_url의 지역 토큰(시·도 로마자 + 주요 시·군) → 시·도 약칭. 없으면 null.
     *  ① 호스트에서 부분일치(www.gongju.go.kr 등)
     *  ② 호스트에 없으면 경로/쿼리 세그먼트에서 정확일치
     *     (happydorm.or.kr/cheonan, ccei.creativekorea.or.kr/incheon 처럼
     *      공용 포털이 시·도를 URL 경로로 구분해 호스트만 보면 전국으로 새는 케이스) */
    private String regionFromHost(String sourceUrl) {
        if (!StringUtils.hasText(sourceUrl)) return null;
        String lower = sourceUrl.toLowerCase();
        Matcher m = HOST_PATTERN.matcher(lower);
        if (m.find()) {
            String host = m.group(1);
            for (Map.Entry<String, String> e : HOST_KEYWORDS.entrySet()) {
                if (host.contains(e.getKey())) return e.getValue();
            }
        }
        // 경로 세그먼트는 정확일치만 — 짧은 토큰(gumi·asan 등)이 다른 단어에 substring으로 오인되는 것을 막는다.
        for (String seg : lower.split("[/?#&=.]")) {
            String region = HOST_KEYWORDS.get(seg);
            if (region != null) return region;
        }
        return null;
    }

    private static final Pattern HOST_PATTERN = Pattern.compile("://([^/]+)");

    // 호스트 부분 문자열 → 시·도. 순서 중요(더 구체적인 토큰 먼저). DB 백필 로직과 동일하게 유지할 것.
    private static final Map<String, String> HOST_KEYWORDS = buildHostKeywords();

    private static Map<String, String> buildHostKeywords() {
        Map<String, String> m = new LinkedHashMap<>();
        // 시·도 로마자(정식·약칭)
        m.put("chungcheongnam", "충남"); m.put("chungnam", "충남");
        m.put("chungcheongbuk", "충북"); m.put("chungbuk", "충북");
        m.put("gyeongsangnam", "경남");  m.put("gyeongnam", "경남");
        m.put("gyeongsangbuk", "경북");  m.put("gyeongbuk", "경북");
        m.put("jeollanam", "전남");      m.put("jeonnam", "전남");
        m.put("jeollabuk", "전북");      m.put("jeonbuk", "전북");
        m.put("gyeonggi", "경기");
        m.put("gangwon", "강원");
        m.put("incheon", "인천");
        m.put("gwangju", "광주");
        m.put("daejeon", "대전");
        m.put("daegu", "대구");
        m.put("busan", "부산");
        m.put("ulsan", "울산");
        m.put("sejong", "세종");
        m.put("seoul", "서울");
        m.put("seogwipo", "제주"); m.put("jeju", "제주");
        // 주요 시·군 도메인 → 상위 시·도
        m.put("chuncheon", "강원"); m.put("wonju", "강원");
        m.put("cheonan", "충남"); m.put("asan", "충남"); m.put("gongju", "충남");
        m.put("hongseong", "충남"); m.put("dangjin", "충남"); m.put("taean", "충남");
        m.put("gyeryong", "충남"); m.put("cndc", "충남"); m.put("cnmusic", "충남");
        m.put("chungju", "충북");
        m.put("changwon", "경남"); m.put("tongyeong", "경남");
        m.put("suncheon", "전남"); m.put("haenam", "전남"); m.put("jangheung", "전남");
        m.put("jbct", "전남"); m.put("jnsinbo", "전남");
        m.put("iksan", "전북"); m.put("jbsinbo", "전북");
        m.put("siheung", "경기"); m.put("uiwang", "경기");
        m.put("gijang", "부산"); m.put("bsbukgu", "부산"); m.put("bsdonggu", "부산");
        m.put("gumi", "경북"); m.put("gbwork", "경북"); m.put("gbyouth", "경북");
        m.put("sjepa", "세종"); m.put("sjcf", "세종"); m.put("sjrise", "세종");
        m.put("sjsinbo", "세종"); m.put("sjyouth", "세종");
        // 대학 도메인(.ac.kr) → 본교 소재 시·도.
        // 대학 주관 사업은 기관명만 보면 전국으로 새지만 실제론 해당 지역 거주자용 지역사업이다.
        // (한국폴리텍 kopo처럼 전국 캠퍼스 단위 기관은 의도적으로 제외해 '전국' 유지)
        m.put("jnu", "전남"); m.put("kongju", "충남"); m.put("andong", "경북");
        m.put("hanseo", "충남"); m.put("hongik", "서울"); m.put("dongguk", "서울");
        return m;
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
