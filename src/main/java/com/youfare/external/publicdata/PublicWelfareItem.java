package com.youfare.external.publicdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공공데이터포털 청년정책 API 응답 단건 항목
 * 실제 API 필드명에 맞게 @JsonProperty로 매핑
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicWelfareItem {

    @JsonProperty("servId")
    private String servId;           // 서비스 ID (externalId)

    @JsonProperty("servNm")
    private String servNm;           // 서비스명 (title)

    @JsonProperty("servDgst")
    private String servDgst;         // 서비스 요약 (description)

    @JsonProperty("lifeNmArray")
    private String lifeNmArray;      // 생애주기 (카테고리 분류에 활용)

    @JsonProperty("tgtrDtlCn")
    private String tgtrDtlCn;        // 지원대상 상세내용

    @JsonProperty("alwServCn")
    private String alwServCn;        // 소득조건

    @JsonProperty("aplyYmd")
    private String aplyYmd;          // 신청시작일 (yyyyMMdd)

    @JsonProperty("aplyEndYmd")
    private String aplyEndYmd;       // 신청종료일 (yyyyMMdd)

    @JsonProperty("inqplCtadrUrl")
    private String inqplCtadrUrl;    // 원본링크

    @JsonProperty("rprsCtadr")
    private String rprsCtadr;        // 지역 (시도명)
}
