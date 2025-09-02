package com.line.library.notification;

import java.time.*;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.line.library.loan.LoanRepository;

@Service
public class DueSoonNotificationService {

    private final LoanRepository loanRepository;
    private final Clock clock;

    @Autowired
    public DueSoonNotificationService(LoanRepository loanRepository) {
        this(loanRepository, Clock.systemDefaultZone());
    }

    // Allow injection for tests
    DueSoonNotificationService(LoanRepository loanRepository, Clock clock) {
        this.loanRepository = loanRepository;
        this.clock = clock;
    }

    public int notifyLoansDueInDaysBatched(int daysAhead, ZoneId zone, int batchSize) {
        LocalDate targetDate = LocalDate.now(clock).plusDays(daysAhead);
        ZonedDateTime startOfDay = targetDate.atStartOfDay(zone);
        ZonedDateTime endOfDay = startOfDay.plusDays(1);

        int total = 0;
        Pageable firstPage = PageRequest.of(0, batchSize, Sort.by(Sort.Direction.ASC, "id"));
        Slice<DueSoonNoticeRow> slice;
        do {
            slice = loanRepository.findDueSoonNoticeRows(startOfDay.toInstant(), endOfDay.toInstant(), firstPage);

            List<DueSoonNoticeRow> rows = slice.getContent();
            if (rows.isEmpty()) break;

            Instant now = Instant.now(clock);
            List<Long> ids = new ArrayList<>(rows.size());
            for (DueSoonNoticeRow row : rows) {
                System.out.println("[DueSoonNotice] Notify " + row.getUserEmail() + ": '" + row.getBookTitle() + "' due at " + row.getDueAt());
                ids.add(row.getLoanId());
            }

            // Mark batch as notified in a single bulk update
            loanRepository.markDueNoticeSent(now, ids);
            total += ids.size();
        } while (slice.hasNext());

        return total;
    }
}
