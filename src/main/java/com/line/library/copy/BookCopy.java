package com.line.library.copy;

import com.line.library.book.Book;
import com.line.library.library.Library;

import jakarta.persistence.*;

@Entity
@Table(name = "book_copies")
public class BookCopy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(optional = false)
    @JoinColumn(name = "library_id")
    private Library library;

    private String shelfLocation;

    // Denormalized pointer to current active loan for this copy (nullable)
    @Column(name = "current_loan_id")
    private Long currentLoanId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Library getLibrary() {
        return library;
    }

    public void setLibrary(Library library) {
        this.library = library;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }

    public Long getCurrentLoanId() {
        return currentLoanId;
    }

    public void setCurrentLoanId(Long currentLoanId) {
        this.currentLoanId = currentLoanId;
    }
}
