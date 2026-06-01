package com.youfare.external.publicdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicWelfareResponse {

    @JsonProperty("wlfareInfoList")
    private List<PublicWelfareItem> wlfareInfoList;

    @JsonProperty("totalCnt")
    private Integer totalCnt;
}
