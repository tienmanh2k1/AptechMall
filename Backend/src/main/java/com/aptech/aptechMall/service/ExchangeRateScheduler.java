package com.aptech.aptechMall.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateScheduler {

    private final ExchangeRateService exchangeRateService;

    // Update daily at midnight (0:00 AM)
    @Scheduled(cron = "0 0 0 * * *")
    public void updateExchangeRatesDaily() {
        log.info("⏰ Scheduled task: Updating exchange rates...");
        exchangeRateService.updateRatesFromApi();
    }
}
