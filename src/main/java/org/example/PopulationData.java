package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PopulationData {
    @JsonProperty("statsYm") private String statsYm;  // 통계년월
    @JsonProperty("mvinAdmmCd") private String mvinAdmmCd;  // 전입 행정기관 코드
    @JsonProperty("mvtAdmmCd") private String mvtAdmmCd;  // 전출 행정기관 코드
    @JsonProperty("totNmprCnt") private String totNmprCnt;  // 총 인구수
    @JsonProperty("maleNmprCnt") private String maleNmprCnt;  // 남자인구수
    @JsonProperty("femlNmprCnt") private String femlNmprCnt;  // 여자인구수
}
