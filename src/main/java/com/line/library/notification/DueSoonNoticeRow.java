package com.line.library.notification;

import java.time.Instant;

// Projection for due-soon notifications to avoid loading full entities
public interface DueSoonNoticeRow {
    Long getLoanId();
    String getUserEmail();
    String getBookTitle();
    Instant getDueAt();
}

