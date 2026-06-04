package com.youfare.external.publicdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 온통청년(youthcenter.go.kr) 청년정책 API(getPlcy) 응답 단건 항목
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicWelfareItem {

    @JsonProperty("plcyNo")
    private String plcyNo;           // 정책 번호 (externalId)

    @JsonProperty("plcyNm")
    private String plcyNm;           // 정책명 (title)

    @JsonProperty("plcyExplnCn")
    private String plcyExplnCn;      // 정책 설명 (description)

    @JsonProperty("plcySprtCn")
    private String plcySprtCn;       // 정책 지원 내용

    @JsonProperty("lclsfNm")
    private String lclsfNm;          // 정책 대분류명 (예: 일자리, 주거, 교육･직업훈련, 금융･복지･문화, 참여･기반)

    @JsonProperty("mclsfNm")
    private String mclsfNm;          // 정책 중분류명

    @JsonProperty("sprvsnInstCdNm")
    private String sprvsnInstCdNm;   // 주관기관명

    @JsonProperty("earnCndSeCd")
    private String earnCndSeCd;      // 소득 조건 구분 코드

    @JsonProperty("earnEtcCn")
    private String earnEtcCn;        // 소득 조건 기타 설명

    @JsonProperty("earnMinAmt")
    private String earnMinAmt;       // 연소득 기준 최소 금액(만원). earnCndSeCd=0043002일 때 의미 있음

    @JsonProperty("earnMaxAmt")
    private String earnMaxAmt;       // 연소득 기준 최대 금액(만원). earnCndSeCd=0043002일 때 의미 있음

    @JsonProperty("sprtTrgtMinAge")
    private Integer sprtTrgtMinAge;  // 지원 대상 최소 연령

    @JsonProperty("sprtTrgtMaxAge")
    private Integer sprtTrgtMaxAge;  // 지원 대상 최대 연령

    @JsonProperty("aplyYmd")
    private String aplyYmd;          // 신청 기간 (예: "20251222 ~ 20261211")

    @JsonProperty("aplyUrlAddr")
    private String aplyUrlAddr;      // 신청 URL

    @JsonProperty("refUrlAddr1")
    private String refUrlAddr1;      // 참고 사이트 URL
}
