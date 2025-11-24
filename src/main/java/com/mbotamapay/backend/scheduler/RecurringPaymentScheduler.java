package com.mbotamapay.backend.scheduler;

import com.mbotamapay.backend.service.RecurringPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringPaymentScheduler {

    private final RecurringPaymentService recurringPaymentService;

    @Scheduled(cron = "0 0 8 * * *") // Run every day at 8 AM
    public void processRecurringPayments() {
        log.info("Scheduler triggered: Processing recurring payments...");
        try {
            recurringPaymentService.processRecurringPayments();
        } catch (Exception e) {
            log.error("Error occurred while processing recurring payments", e);
        }
        log.info("Scheduler finished: Recurring payments processed.");
    }
}
