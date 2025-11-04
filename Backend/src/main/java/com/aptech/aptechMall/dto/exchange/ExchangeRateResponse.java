package com.aptech.aptechMall.dto.exchange;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateResponse {
    private String currency;
    private BigDecimal rateToVnd;
    private String source;
    private LocalDateTime updatedAt;
}
