package com.line.library.loan;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import com.line.library.book.BookType;
import com.line.library.copy.BookCopy;
import com.line.library.user.User;
import com.line.library.notification.DueSoonNoticeRow;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    boolean existsByCopyAndReturnedAtIsNull(BookCopy copy);

    List<Loan> findByUserOrderByReturnedAtDescDueAtAsc(User user);

    long countByUserAndCopyBookTypeAndReturnedAtIsNull(User user, BookType type);

    // Scheduler helper: loans due on a target day, not returned, and not yet notified (paged)
    Slice<Loan> findByReturnedAtIsNullAndDueAtBetweenAndDueNoticeSentAtIsNull(
        Instant startInclusive,
        Instant endExclusive,
        Pageable pageable
    );

    // Optimized read for notifications: fetch only fields needed via projection
    @Query("select l.id as loanId, u.email as userEmail, b.title as bookTitle, l.dueAt as dueAt " +
           "from Loan l join l.user u join l.copy c join c.book b " +
           "where l.returnedAt is null and l.dueNoticeSentAt is null " +
           "and l.dueAt >= :startInclusive and l.dueAt < :endExclusive " +
           "order by l.id asc")
    Slice<DueSoonNoticeRow> findDueSoonNoticeRows(
        @Param("startInclusive") Instant startInclusive,
        @Param("endExclusive") Instant endExclusive,
        Pageable pageable
    );

    // Bulk update to mark notifications as sent
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Loan l set l.dueNoticeSentAt = :now where l.id in :ids")
    int markDueNoticeSent(
        @Param("now") Instant now,
        @Param("ids") java.util.Collection<Long> ids
    );
}
