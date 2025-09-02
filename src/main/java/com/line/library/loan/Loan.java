package com.line.library.loan;

import java.time.Instant;

import com.line.library.copy.BookCopy;
import com.line.library.user.User;

import jakarta.persistence.*;

@Entity
@Table(name = "loans")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "copy_id")
    private BookCopy copy;

    @Column(nullable = false)
    private Instant loanedAt = Instant.now();

    @Column(nullable = false)
    private Instant dueAt;

    private Instant returnedAt;

    @Column(name = "due_notice_sent_at")
    private Instant dueNoticeSentAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BookCopy getCopy() {
        return copy;
    }

    public void setCopy(BookCopy copy) {
        this.copy = copy;
    }

    public Instant getLoanedAt() {
        return loanedAt;
    }

    public void setLoanedAt(Instant loanedAt) {
        this.loanedAt = loanedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(Instant returnedAt) {
        this.returnedAt = returnedAt;
    }

    public Instant getDueNoticeSentAt() {
        return dueNoticeSentAt;
    }

    public void setDueNoticeSentAt(Instant dueNoticeSentAt) {
        this.dueNoticeSentAt = dueNoticeSentAt;
    }
}
