package com.youfare.external.publicdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 온통청년(youthcenter.go.kr) 청년정책 API(getPlcy) 응답 래퍼.
 * 구조: { resultCode, resultMessage, result: { pagging, youthPolicyList } }
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicWelfareResponse {

    @JsonProperty("resultCode")
    private Integer resultCode;

    @JsonProperty("resultMessage")
    private String resultMessage;

    @JsonProperty("result")
    private Result result;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {

        @JsonProperty("pagging")
        private Pagging pagging;

        @JsonProperty("youthPolicyList")
        private List<PublicWelfareItem> youthPolicyList;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagging {

        @JsonProperty("totCount")
        private Integer totCount;

        @JsonProperty("pageNum")
        private Integer pageNum;

        @JsonProperty("pageSize")
        private Integer pageSize;
    }
}
