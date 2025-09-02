package com.line.library.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import com.line.library.book.Book;
import com.line.library.book.BookRepository;
import com.line.library.book.BookType;
import com.line.library.copy.BookCopy;
import com.line.library.copy.BookCopyRepository;
import com.line.library.library.Library;
import com.line.library.library.LibraryRepository;
import com.line.library.loan.Loan;
import com.line.library.loan.LoanRepository;
import com.line.library.user.User;
import com.line.library.user.UserRepository;
import static com.line.library.testutil.TestDataFactory.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        // Avoid executing Postgres-specific schema.sql in embedded tests
        "spring.sql.init.mode=never",
        // Have Hibernate create tables from entities
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DueSoonNotificationServiceTest {

    @Autowired LoanRepository loanRepository;
    @Autowired UserRepository userRepository;
    @Autowired LibraryRepository libraryRepository;
    @Autowired BookRepository bookRepository;
    @Autowired BookCopyRepository copyRepository;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2024-01-10T12:00:00Z"), ZoneOffset.UTC);

    private PrintStream originalOut;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void notifiesAndMarksDueNoticeForLoansDueInFiveDays() {
        // Arrange data
        User user = persistUser(userRepository, "Alice", "alice@example.com");
        Library lib = persistLibrary(libraryRepository, "Main");
        Book book = persistBook(bookRepository, BookType.BOOK);
        BookCopy copy = persistCopy(copyRepository, book, lib);

        // Target window: today (2024-01-10 UTC) + 5 days = 2024-01-15 UTC
        Loan dueInFive = persistActiveLoan(
                loanRepository, user, copy,
                Instant.parse("2024-01-09T08:00:00Z"),
                Instant.parse("2024-01-15T08:00:00Z")
        );

        // Control: outside window, should not be notified
        Loan dueTomorrow = persistActiveLoan(
                loanRepository, user, copy,
                Instant.parse("2024-01-09T08:00:00Z"),
                Instant.parse("2024-01-11T09:00:00Z")
        );

        DueSoonNotificationService svc = new DueSoonNotificationService(loanRepository, fixedClock);

        // Act
        int count = svc.notifyLoansDueInDaysBatched(5, ZoneId.of("UTC"), 10);

        // Assert
        assertThat(count).isEqualTo(1);

        Loan refreshed = loanRepository.findById(dueInFive.getId()).orElseThrow();
        assertThat(refreshed.getDueNoticeSentAt()).isNotNull();

        Loan untouched = loanRepository.findById(dueTomorrow.getId()).orElseThrow();
        assertThat(untouched.getDueNoticeSentAt()).isNull();

        String printed = outContent.toString();
        assertThat(printed)
                .contains("[DueSoonNotice] Notify")
                .contains("alice@example.com")
                .contains("Clean Code")
                .contains("2024-01-15");
    }
}
