package com.line.library.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.line.library.book.Book;
import com.line.library.book.BookRepository;
import com.line.library.book.BookType;
import com.line.library.copy.BookCopyRepository;
import com.line.library.copy.BookCopy;
import com.line.library.library.Library;
import com.line.library.library.LibraryRepository;
import com.line.library.security.AuthUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Controller
public class BooksController {

    private final BookRepository bookRepository;
    private final LibraryRepository libraryRepository;
    private final BookCopyRepository copyRepository;

    public BooksController(BookRepository bookRepository,
                               LibraryRepository libraryRepository,
                               BookCopyRepository copyRepository) {
        this.bookRepository = bookRepository;
        this.libraryRepository = libraryRepository;
        this.copyRepository = copyRepository;
    }

    @GetMapping(value = "/books", produces = MediaType.TEXT_HTML_VALUE)
    public String books(@RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "pubYear", required = false) Integer pubYear,
                        @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                        Model model) {
        List<Library> libraries = libraryRepository.findAll();

        String query = (q != null && !q.isBlank()) ? q.trim() : null;
        Integer y = pubYear;

        // Only treat as search when a query is provided; year alone is ignored
        boolean isSearch = (query != null);
        // Guard: extremely short queries can cause seq scans with pg_trgm
        boolean tooShortSearch = isSearch && query.length() < 3;

        // Enforce max 10 results per page
        int size = 10;
        int safePage = (page == null || page < 0) ? 0 : page;
        Pageable pageable = isSearch
                ? PageRequest.of(safePage, size) // ordering handled in the query
                : PageRequest.of(safePage, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Book> pageResult;
        long t0 = System.nanoTime();
        if (isSearch) {
            if (tooShortSearch) {
                // avoid hitting DB to prevent full scan
                pageResult = Page.empty(pageable);
            } else {
                pageResult = bookRepository.searchFuzzyAnyPaged(query, y, pageable);
            }
        } else {
            pageResult = bookRepository.findAll(pageable);
        }
        long queryTimeMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

        List<Book> books = pageResult.getContent();

        // Fetch available copy counts for all books in a single query
        List<Long> bookIds = books.stream().map(Book::getId).toList();
        Map<Long, Map<Long, Long>> availabilityMap = new LinkedHashMap<>();
        if (!bookIds.isEmpty()) {
            List<BookCopyRepository.AvailableCount> counts = copyRepository.countAvailableByBookIds(bookIds);
            for (BookCopyRepository.AvailableCount c : counts) {
                availabilityMap
                    .computeIfAbsent(c.getBookId(), k -> new LinkedHashMap<>())
                    .put(c.getLibraryId(), c.getAvailable());
            }
        }

        List<BookAvailabilityRow> rows = books.stream().map(book -> {
            Map<Long, Long> perLibrary = new LinkedHashMap<>();
            Map<Long, Long> byLibrary = availabilityMap.getOrDefault(book.getId(), Map.of());
            for (Library lib : libraries) {
                long available = byLibrary.getOrDefault(lib.getId(), 0L);
                perLibrary.put(lib.getId(), available);
            }
            return new BookAvailabilityRow(book, perLibrary);
        }).toList();

        model.addAttribute("libraries", libraries);
        model.addAttribute("rows", rows);
        model.addAttribute("q", q);
        model.addAttribute("pubYear", pubYear);
        model.addAttribute("isSearch", isSearch);
        model.addAttribute("searchTooShort", tooShortSearch);
        model.addAttribute("queryTimeMs", queryTimeMs);
        
        // Pagination metadata
        int currentPage = pageResult.getNumber();
        int totalPages = pageResult.getTotalPages();
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", pageResult.getTotalElements());
        model.addAttribute("pageSize", pageResult.getSize());

        // Build a compact window of page indices to display (0-based)
        // Show up to 4 page links around the current page, with ellipses when appropriate.
        int window = 4;
        if (totalPages > 0) {
            int start = Math.max(0, currentPage - (window / 2));
            int end = Math.min(totalPages - 1, start + window - 1);
            // readjust start when close to the end so we keep a full window
            start = Math.max(0, end - window + 1);

            List<Integer> pagesWindow = java.util.stream.IntStream
                    .rangeClosed(start, end)
                    .boxed()
                    .toList();

            boolean showLeftEllipsis = start > 0;
            boolean showRightEllipsis = end < (totalPages - 1);

            model.addAttribute("pagesWindow", pagesWindow);
            model.addAttribute("showLeftEllipsis", showLeftEllipsis);
            model.addAttribute("showRightEllipsis", showRightEllipsis);
        }
        return "books";
    }

    public record BookAvailabilityRow(Book book, Map<Long, Long> availableByLibraryId) { }

    @GetMapping(value = "/books/new", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasRole('LIBRARIAN')")
    public String newBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("types", BookType.values());
        return "book_form";
    }

    @PostMapping(value = "/books", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("hasRole('LIBRARIAN')")
    public String createBook(@ModelAttribute Book book) {
        Book saved = bookRepository.save(book);
        return "redirect:/books/" + saved.getId();
    }

    @GetMapping(value = "/books/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String viewBook(@PathVariable Long id, Model model, org.springframework.security.core.Authentication auth) {
        Book book = bookRepository.findById(id).orElseThrow();
        List<BookCopy> copies = copyRepository.findByBook(book);
        Set<Long> loanedCopyIds = copies.stream()
                .filter(c -> c.getCurrentLoanId() != null)
                .map(BookCopy::getId)
                .collect(Collectors.toSet());

        model.addAttribute("book", book);
        model.addAttribute("copies", copies);
        model.addAttribute("loanedCopyIds", loanedCopyIds);
        return "book_detail";
    }

    @GetMapping(value = "/books/{id}/edit", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasRole('LIBRARIAN')")
    public String editBook(@PathVariable Long id, Model model) {
        Book book = bookRepository.findById(id).orElseThrow();
        List<Library> libraries = libraryRepository.findAll();

        model.addAttribute("book", book);
        model.addAttribute("types", BookType.values());
        model.addAttribute("libraries", libraries);
        return "book_edit";
    }

    @PostMapping(value = "/books/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("hasRole('LIBRARIAN')")
    public String updateBook(@PathVariable Long id,
                             @ModelAttribute Book form,
                             org.springframework.security.core.Authentication auth) {
        Book book = bookRepository.findById(id).orElseThrow();
        book.setTitle(form.getTitle());
        book.setAuthor(form.getAuthor());
        book.setPubYear(form.getPubYear());
        book.setType(form.getType() != null ? form.getType() : BookType.BOOK);
        bookRepository.save(book);
        return "redirect:/books/" + id;
    }

    @PostMapping(value = "/books/{id}/copies", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("hasRole('LIBRARIAN')")
    public String addCopy(@PathVariable Long id,
                          @RequestParam("libraryId") Long libraryId,
                          @RequestParam(value = "shelfLocation", required = false) String shelfLocation) {
        Book book = bookRepository.findById(id).orElseThrow();
        Library library = libraryRepository.findById(libraryId).orElseThrow();

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setLibrary(library);
        copy.setShelfLocation(shelfLocation);
        copyRepository.save(copy);

        return "redirect:/books/" + id;
    }

    
}
