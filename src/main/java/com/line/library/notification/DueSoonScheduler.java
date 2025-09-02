package com.line.library.notification;

import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DueSoonScheduler {

    private final DueSoonNotificationService notificationService;

    public DueSoonScheduler(DueSoonNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Run daily at 09:00 local time
    @Scheduled(cron = "0 0 9 * * *")
    public void runDaily() {
        int batchSize = 200; // tune as needed
        int count = notificationService.notifyLoansDueInDaysBatched(5, ZoneId.systemDefault(), batchSize);
        if (count > 0) {
            System.out.println("[DueSoonNotice] Sent notifications: " + count);
        }
    }
}
