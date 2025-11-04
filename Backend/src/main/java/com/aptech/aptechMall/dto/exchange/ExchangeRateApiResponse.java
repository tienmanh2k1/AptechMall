package com.aptech.aptechMall.dto.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
public class ExchangeRateApiResponse {
    @JsonProperty("base")
    private String base;

    @JsonProperty("rates")
    private Map<String, Double> rates;

    @JsonProperty("time_last_updated")
    private Long timeLastUpdated;
}
