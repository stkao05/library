package com.line.library.copy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.line.library.book.Book;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    List<BookCopy> findByBook(Book book);

    @Query("""
        SELECT c.book.id AS bookId,
               c.library.id AS libraryId,
               COUNT(c) AS available
        FROM BookCopy c
        WHERE c.book.id IN :bookIds
          AND c.currentLoanId IS NULL
        GROUP BY c.book.id, c.library.id
        """)
    List<AvailableCount> countAvailableByBookIds(@Param("bookIds") List<Long> bookIds);

    interface AvailableCount {
        Long getBookId();
        Long getLibraryId();
        long getAvailable();
    }

    // Locked lookup to ensure exclusive access to a copy row during loan operations
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        // Use NOWAIT semantics where supported (Postgres, Oracle, MySQL 8+),
        // otherwise a 0ms lock timeout to fail fast instead of blocking
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")
    })
    @Query("select c from BookCopy c where c.id = :id")
    java.util.Optional<BookCopy> findByIdForUpdate(@Param("id") Long id);
}
