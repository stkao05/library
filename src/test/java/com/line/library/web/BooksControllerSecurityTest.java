package com.line.library.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import com.line.library.book.Book;
import com.line.library.book.BookRepository;
import com.line.library.book.BookType;
import com.line.library.config.SecurityConfig;
import com.line.library.copy.BookCopyRepository;
import com.line.library.library.Library;
import com.line.library.library.LibraryRepository;

@WebMvcTest(controllers = BooksController.class)
@Import(SecurityConfig.class)
class BooksControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean BookRepository bookRepository;
    @MockitoBean LibraryRepository libraryRepository;
    @MockitoBean BookCopyRepository copyRepository;

    // GET /books/new
    @Test
    void getNewBookForm_redirectsToLogin_whenAnonymous() throws Exception {
        mockMvc.perform(get("/books/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void getNewBookForm_forbidden_whenMember() throws Exception {
        mockMvc.perform(get("/books/new").with(user("mem").roles("MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getNewBookForm_ok_whenLibrarian() throws Exception {
        mockMvc.perform(get("/books/new").with(user("lib").roles("LIBRARIAN")))
                .andExpect(status().isOk());
    }

    // POST /books
    @Test
    void createBook_redirectsToLogin_whenAnonymous() throws Exception {
        mockMvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED).with(csrf())
                        .param("title", "T")
                        .param("author", "A")
                        .param("pubYear", "2020")
                        .param("type", BookType.BOOK.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void createBook_forbidden_whenMember() throws Exception {
        mockMvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(user("mem").roles("MEMBER")).with(csrf())
                        .param("title", "T")
                        .param("author", "A")
                        .param("pubYear", "2020")
                        .param("type", BookType.BOOK.name()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBook_redirects_whenLibrarian() throws Exception {
        Book saved = new Book();
        saved.setId(42L);
        saved.setTitle("T");
        saved.setAuthor("A");
        saved.setType(BookType.BOOK);
        when(bookRepository.save(any(Book.class))).thenReturn(saved);

        mockMvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(user("lib").roles("LIBRARIAN")).with(csrf())
                        .param("title", "T")
                        .param("author", "A")
                        .param("pubYear", "2020")
                        .param("type", BookType.BOOK.name()))
                .andExpect(status().is3xxRedirection());
    }

    // GET /books/{id}/edit
    @Test
    void editBook_redirectsToLogin_whenAnonymous() throws Exception {
        mockMvc.perform(get("/books/1/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void editBook_forbidden_whenMember() throws Exception {
        mockMvc.perform(get("/books/1/edit").with(user("mem").roles("MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void editBook_ok_whenLibrarian() throws Exception {
        Book b = new Book();
        b.setId(1L);
        b.setTitle("T");
        b.setAuthor("A");
        b.setType(BookType.BOOK);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(b));
        when(libraryRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/books/1/edit").with(user("lib").roles("LIBRARIAN")))
                .andExpect(status().isOk());
    }

    // POST /books/{id}
    @Test
    void updateBook_redirectsToLogin_whenAnonymous() throws Exception {
        mockMvc.perform(post("/books/1").contentType(MediaType.APPLICATION_FORM_URLENCODED).with(csrf())
                        .param("title", "T2")
                        .param("author", "A2")
                        .param("pubYear", "2021")
                        .param("type", BookType.BOOK.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void updateBook_forbidden_whenMember() throws Exception {
        mockMvc.perform(post("/books/1").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(user("mem").roles("MEMBER")).with(csrf())
                        .param("title", "T2")
                        .param("author", "A2")
                        .param("pubYear", "2021")
                        .param("type", BookType.BOOK.name()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateBook_redirects_whenLibrarian() throws Exception {
        Book existing = new Book();
        existing.setId(1L);
        existing.setTitle("T");
        existing.setAuthor("A");
        existing.setType(BookType.BOOK);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenReturn(existing);

        mockMvc.perform(post("/books/1").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(user("lib").roles("LIBRARIAN")).with(csrf())
                        .param("title", "T2")
                        .param("author", "A2")
                        .param("pubYear", "2021")
                        .param("type", BookType.BOOK.name()))
                .andExpect(status().is3xxRedirection());
    }

    // POST /books/{id}/copies
    @Test
    void addCopy_redirectsToLogin_whenAnonymous() throws Exception {
        mockMvc.perform(post("/books/1/copies").contentType(MediaType.APPLICATION_FORM_URLENCODED).with(csrf())
                        .param("libraryId", "2")
                        .param("shelfLocation", "A-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void addCopy_forbidden_whenMember() throws Exception {
        mockMvc.perform(post("/books/1/copies").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(user("mem").roles("MEMBER")).with(csrf())
                        .param("libraryId", "2")
                        .param("shelfLocation", "A-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addCopy_redirects_whenLibrarian() throws Exception {
        Book b = new Book();
        b.setId(1L);
        b.setTitle("T");
        b.setAuthor("A");
        b.setType(BookType.BOOK);
        Library lib = new Library();
        lib.setId(2L);
        lib.setName("Main");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(b));
        when(libraryRepository.findById(2L)).thenReturn(Optional.of(lib));

        mockMvc.perform(post("/books/1/copies").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(user("lib").roles("LIBRARIAN")).with(csrf())
                        .param("libraryId", "2")
                        .param("shelfLocation", "A-1"))
                .andExpect(status().is3xxRedirection());
    }
}
