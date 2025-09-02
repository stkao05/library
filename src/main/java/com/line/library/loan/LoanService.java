package com.line.library.loan;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.line.library.book.BookType;
import com.line.library.copy.BookCopy;
import com.line.library.copy.BookCopyRepository;
import com.line.library.user.User;
import com.line.library.user.UserRepository;

@Service
public class LoanService {

    public static final int MAX_ACTIVE_BOOKS = 5;
    public static final int MAX_ACTIVE_PUBLICATIONS = 10;

    private final LoanRepository loanRepository;
    private final BookCopyRepository copyRepository;
    private final UserRepository userRepository;

    public LoanService(LoanRepository loanRepository,
                         BookCopyRepository copyRepository,
                         UserRepository userRepository) {
        this.loanRepository = loanRepository;
        this.copyRepository = copyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Loan loanCopy(Long copyId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        BookCopy copy = copyRepository.findByIdForUpdate(copyId).orElseThrow();

        if (copy.getCurrentLoanId() != null || loanRepository.existsByCopyAndReturnedAtIsNull(copy)) {
            throw new IllegalStateException("Copy is already loaned");
        }

        BookType type = copy.getBook().getType();
        if (type == BookType.BOOK) {
            long activeBooks = loanRepository.countByUserAndCopyBookTypeAndReturnedAtIsNull(user, BookType.BOOK);
            if (activeBooks >= MAX_ACTIVE_BOOKS) {
                throw new IllegalStateException(String.format(
                        "Borrowing limit reached: max %d books", MAX_ACTIVE_BOOKS));
            }
        } else if (type == BookType.PUBLICATION) {
            long activePubs = loanRepository.countByUserAndCopyBookTypeAndReturnedAtIsNull(user, BookType.PUBLICATION);
            if (activePubs >= MAX_ACTIVE_PUBLICATIONS) {
                throw new IllegalStateException(String.format(
                        "Borrowing limit reached: max %d publications", MAX_ACTIVE_PUBLICATIONS));
            }
        }

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setCopy(copy);
        loan.setLoanedAt(Instant.now());
        loan.setDueAt(Instant.now().plus(30, ChronoUnit.DAYS));
        Loan saved = loanRepository.save(loan);

        copy.setCurrentLoanId(saved.getId());
        copyRepository.save(copy);

        return saved;
    }

    @Transactional
    public Loan returnLoan(Long loanId, String userEmail) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();

        if (!loan.getUser().getEmail().equals(userEmail)) {
            throw new IllegalStateException("You cannot return another user's loan");
        }

        if (loan.getReturnedAt() != null) {
            throw new IllegalStateException("Loan has already been returned");
        }

        loan.setReturnedAt(Instant.now());
        Loan saved = loanRepository.save(loan);

        // Clear denormalized field if it points to this loan
        BookCopy copy = loan.getCopy();
        if (copy.getCurrentLoanId() != null && copy.getCurrentLoanId().equals(saved.getId())) {
            copy.setCurrentLoanId(null);
            copyRepository.save(copy);
        }

        return saved;
    }
}
