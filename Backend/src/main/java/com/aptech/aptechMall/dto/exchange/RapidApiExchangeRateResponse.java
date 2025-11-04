package com.aptech.aptechMall.dto.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RapidApiExchangeRateResponse {
    @JsonProperty("code")
    private String code;

    @JsonProperty("msg")
    private String message;

    @JsonProperty("convert_result")
    private ConvertResult convertResult;

    @JsonProperty("time_update")
    private TimeUpdate timeUpdate;

    @Data
    public static class ConvertResult {
        @JsonProperty("base")
        private String base;

        @JsonProperty("target")
        private String target;

        @JsonProperty("rate")
        private Double rate;
    }

    @Data
    public static class TimeUpdate {
        @JsonProperty("time_unix")
        private Long timeUnix;

        @JsonProperty("time_utc")
        private String timeUtc;

        @JsonProperty("time_zone")
        private String timeZone;
    }
}
