package com.line.library.testutil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

public final class TestDataFactory {
    private TestDataFactory() {}

    public static User persistUser(UserRepository users, String name, String email) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash("x");
        return users.save(u);
    }

    public static User persistUser(UserRepository users, String email) {
        return persistUser(users, "Alice", email);
    }

    public static Library persistLibrary(LibraryRepository libraries, String name) {
        Library l = new Library();
        l.setName(name);
        return libraries.save(l);
    }

    public static Book persistBook(BookRepository books, BookType type) {
        Book b = new Book();
        b.setTitle(type == BookType.BOOK ? "Clean Code" : "Science Weekly");
        b.setAuthor("Author");
        b.setType(type);
        return books.save(b);
    }

    public static BookCopy persistCopy(BookCopyRepository copies, Book book, Library lib) {
        BookCopy c = new BookCopy();
        c.setBook(book);
        c.setLibrary(lib);
        return copies.save(c);
    }

    public static Loan persistActiveLoan(LoanRepository loans, User user, BookCopy copy, Instant loanedAt, Instant dueAt) {
        Loan l = new Loan();
        l.setUser(user);
        l.setCopy(copy);
        l.setLoanedAt(loanedAt);
        l.setDueAt(dueAt);
        return loans.save(l);
    }

    public static Loan persistActiveLoanDefaultWindow(LoanRepository loans, User user, BookCopy copy) {
        Instant loanedAt = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant dueAt = Instant.now().plus(10, ChronoUnit.DAYS);
        return persistActiveLoan(loans, user, copy, loanedAt, dueAt);
    }
}

