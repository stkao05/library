package com.line.library.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
class LoanServiceTest {

    @Autowired LoanRepository loanRepository;
    @Autowired BookCopyRepository copyRepository;
    @Autowired UserRepository userRepository;
    @Autowired BookRepository bookRepository;
    @Autowired LibraryRepository libraryRepository;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(loanRepository, copyRepository, userRepository);
    }

    @Test
    void loanCopy_success_whenUnderTypeLimit() {
        User user = persistUser(userRepository, "alice@example.com");
        Library lib = persistLibrary(libraryRepository, "Main");
        Book book = persistBook(bookRepository, BookType.BOOK);

        // Existing active loans below limit
        for (int i = 0; i < 3; i++) {
            BookCopy c = persistCopy(copyRepository, book, lib);
            persistActiveLoanDefaultWindow(loanRepository, user, c);
        }

        BookCopy targetCopy = persistCopy(copyRepository, book, lib);

        Loan created = loanService.loanCopy(targetCopy.getId(), user.getEmail());

        assertThat(created.getId()).isNotNull();
        assertThat(created.getUser().getId()).isEqualTo(user.getId());
        assertThat(created.getCopy().getId()).isEqualTo(targetCopy.getId());
        assertThat(created.getLoanedAt()).isNotNull();
        assertThat(created.getDueAt()).isNotNull();
        assertThat(created.getDueAt()).isAfter(created.getLoanedAt());

        // Verify copy denormalized field persisted
        BookCopy refreshed = copyRepository.findById(targetCopy.getId()).orElseThrow();
        assertThat(refreshed.getCurrentLoanId()).isEqualTo(created.getId());

        long activeBooks = loanRepository.countByUserAndCopyBookTypeAndReturnedAtIsNull(user, BookType.BOOK);
        assertThat(activeBooks).isEqualTo(4);
    }

    @Test
    void loanCopy_fails_whenCopyAlreadyLoaned() {
        User user = persistUser(userRepository, "alice@example.com");
        Library lib = persistLibrary(libraryRepository, "Main");
        Book book = persistBook(bookRepository, BookType.BOOK);
        BookCopy copy = persistCopy(copyRepository, book, lib);

        // Pre-existing active loan for the same copy
        persistActiveLoan(loanRepository, user, copy,
                Instant.now().minus(2, ChronoUnit.DAYS),
                Instant.now().plus(10, ChronoUnit.DAYS));

        assertThatThrownBy(() -> loanService.loanCopy(copy.getId(), user.getEmail()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Copy is already loaned");

        // Ensure no additional loan was created
        long count = loanRepository.count();
        assertThat(count).isEqualTo(1);

        // currentLoanId was not set when we manually created the loan
        BookCopy refreshed = copyRepository.findById(copy.getId()).orElseThrow();
        assertThat(refreshed.getCurrentLoanId()).isNull();
    }

    @Test
    void loanCopy_fails_whenBookLimitReached() {
        User user = persistUser(userRepository, "alice@example.com");
        Library lib = persistLibrary(libraryRepository, "Main");
        Book book = persistBook(bookRepository, BookType.BOOK);

        // Create active loans at the limit
        for (int i = 0; i < LoanService.MAX_ACTIVE_BOOKS; i++) {
            BookCopy c = persistCopy(copyRepository, book, lib);
            persistActiveLoanDefaultWindow(loanRepository, user, c);
        }

        BookCopy target = persistCopy(copyRepository, book, lib);

        assertThatThrownBy(() -> loanService.loanCopy(target.getId(), user.getEmail()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Borrowing limit reached");

        long activeBooks = loanRepository.countByUserAndCopyBookTypeAndReturnedAtIsNull(user, BookType.BOOK);
        assertThat(activeBooks).isEqualTo(LoanService.MAX_ACTIVE_BOOKS);
    }

    @Test
    void loanCopy_fails_whenPublicationLimitReached() {
        User user = persistUser(userRepository, "alice@example.com");
        Library lib = persistLibrary(libraryRepository, "Main");
        Book pub = persistBook(bookRepository, BookType.PUBLICATION);

        // Create active loans at the limit
        for (int i = 0; i < LoanService.MAX_ACTIVE_PUBLICATIONS; i++) {
            BookCopy c = persistCopy(copyRepository, pub, lib);
            persistActiveLoanDefaultWindow(loanRepository, user, c);
        }

        BookCopy target = persistCopy(copyRepository, pub, lib);

        assertThatThrownBy(() -> loanService.loanCopy(target.getId(), user.getEmail()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Borrowing limit reached");

        long activePubs = loanRepository.countByUserAndCopyBookTypeAndReturnedAtIsNull(user, BookType.PUBLICATION);
        assertThat(activePubs).isEqualTo(LoanService.MAX_ACTIVE_PUBLICATIONS);
    }

    // Helpers have been centralized in TestDataFactory
}
