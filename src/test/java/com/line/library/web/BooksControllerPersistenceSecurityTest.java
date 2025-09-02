package com.line.library.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import com.line.library.book.Book;
import com.line.library.book.BookRepository;
import com.line.library.book.BookType;
import com.line.library.copy.BookCopyRepository;
import com.line.library.library.Library;
import com.line.library.library.LibraryRepository;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        // Avoid executing Postgres-specific schema.sql in embedded tests
        "spring.sql.init.mode=never",
        // Have Hibernate create tables from entities
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BooksControllerPersistenceSecurityTest {

    @Autowired MockMvc mockMvc;
    @Autowired BookRepository bookRepository;
    @Autowired LibraryRepository libraryRepository;
    @Autowired BookCopyRepository copyRepository;

    @BeforeEach
    void clean() {
        copyRepository.deleteAll();
        bookRepository.deleteAll();
        libraryRepository.deleteAll();
    }

    // POST /books
    @Test
    void createBook_persists_whenLibrarian() throws Exception {
        long before = bookRepository.count();

        mockMvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(user("lib").roles("LIBRARIAN")).with(csrf())
                        .param("title", "T")
                        .param("author", "A")
                        .param("pubYear", "2020")
                        .param("type", BookType.BOOK.name()))
                .andExpect(status().is3xxRedirection());

        assertThat(bookRepository.count()).isEqualTo(before + 1);
        Book saved = bookRepository.findAll().get(0);
        assertThat(saved.getTitle()).isEqualTo("T");
        assertThat(saved.getAuthor()).isEqualTo("A");
        assertThat(saved.getType()).isEqualTo(BookType.BOOK);
    }

    @Test
    void createBook_notPersisted_whenMember() throws Exception {
        long before = bookRepository.count();

        mockMvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(user("mem").roles("MEMBER")).with(csrf())
                        .param("title", "T")
                        .param("author", "A")
                        .param("pubYear", "2020")
                        .param("type", BookType.BOOK.name()))
                .andExpect(status().isForbidden());

        assertThat(bookRepository.count()).isEqualTo(before);
    }

    @Test
    void createBook_notPersisted_whenAnonymous() throws Exception {
        long before = bookRepository.count();

        mockMvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf())
                        .param("title", "T")
                        .param("author", "A")
                        .param("pubYear", "2020")
                        .param("type", BookType.BOOK.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        assertThat(bookRepository.count()).isEqualTo(before);
    }

    // POST /books/{id}
    @Test
    void updateBook_persistsChanges_whenLibrarian() throws Exception {
        Book existing = new Book();
        existing.setTitle("Old");
        existing.setAuthor("Auth");
        existing.setType(BookType.BOOK);
        existing = bookRepository.save(existing);

        mockMvc.perform(post("/books/" + existing.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(user("lib").roles("LIBRARIAN")).with(csrf())
                        .param("title", "NewTitle")
                        .param("author", "NewAuthor")
                        .param("pubYear", "2021")
                        .param("type", BookType.BOOK.name()))
                .andExpect(status().is3xxRedirection());

        Book refreshed = bookRepository.findById(existing.getId()).orElseThrow();
        assertThat(refreshed.getTitle()).isEqualTo("NewTitle");
        assertThat(refreshed.getAuthor()).isEqualTo("NewAuthor");
        assertThat(refreshed.getPubYear()).isEqualTo(2021);
    }

    @Test
    void updateBook_notChanged_whenMember() throws Exception {
        Book existing = new Book();
        existing.setTitle("Old");
        existing.setAuthor("Auth");
        existing.setType(BookType.BOOK);
        existing = bookRepository.save(existing);

        mockMvc.perform(post("/books/" + existing.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(user("mem").roles("MEMBER")).with(csrf())
                        .param("title", "NewTitle")
                        .param("author", "NewAuthor")
                        .param("pubYear", "2021")
                        .param("type", BookType.BOOK.name()))
                .andExpect(status().isForbidden());

        Book refreshed = bookRepository.findById(existing.getId()).orElseThrow();
        assertThat(refreshed.getTitle()).isEqualTo("Old");
        assertThat(refreshed.getAuthor()).isEqualTo("Auth");
        assertThat(refreshed.getPubYear()).isNull();
    }

    @Test
    void updateBook_notChanged_whenAnonymous() throws Exception {
        Book existing = new Book();
        existing.setTitle("Old");
        existing.setAuthor("Auth");
        existing.setType(BookType.BOOK);
        existing = bookRepository.save(existing);

        mockMvc.perform(post("/books/" + existing.getId())
                        .with(csrf())
                        .param("title", "NewTitle")
                        .param("author", "NewAuthor")
                        .param("pubYear", "2021")
                        .param("type", BookType.BOOK.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        Book refreshed = bookRepository.findById(existing.getId()).orElseThrow();
        assertThat(refreshed.getTitle()).isEqualTo("Old");
        assertThat(refreshed.getAuthor()).isEqualTo("Auth");
        assertThat(refreshed.getPubYear()).isNull();
    }

    // POST /books/{id}/copies
    @Test
    void addCopy_persists_whenLibrarian() throws Exception {
        Book b = new Book();
        b.setTitle("T");
        b.setAuthor("A");
        b.setType(BookType.BOOK);
        b = bookRepository.save(b);

        Library lib = new Library();
        lib.setName("Main");
        lib = libraryRepository.save(lib);

        long before = copyRepository.count();

        mockMvc.perform(post("/books/" + b.getId() + "/copies")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(user("lib").roles("LIBRARIAN")).with(csrf())
                        .param("libraryId", lib.getId().toString())
                        .param("shelfLocation", "A-1"))
                .andExpect(status().is3xxRedirection());

        assertThat(copyRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void addCopy_notPersisted_whenMember() throws Exception {
        Book b = new Book();
        b.setTitle("T");
        b.setAuthor("A");
        b.setType(BookType.BOOK);
        b = bookRepository.save(b);

        Library lib = new Library();
        lib.setName("Main");
        lib = libraryRepository.save(lib);

        long before = copyRepository.count();

        mockMvc.perform(post("/books/" + b.getId() + "/copies")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(user("mem").roles("MEMBER")).with(csrf())
                        .param("libraryId", lib.getId().toString())
                        .param("shelfLocation", "A-1"))
                .andExpect(status().isForbidden());

        assertThat(copyRepository.count()).isEqualTo(before);
    }

    @Test
    void addCopy_notPersisted_whenAnonymous() throws Exception {
        Book b = new Book();
        b.setTitle("T");
        b.setAuthor("A");
        b.setType(BookType.BOOK);
        b = bookRepository.save(b);

        Library lib = new Library();
        lib.setName("Main");
        lib = libraryRepository.save(lib);

        long before = copyRepository.count();

        mockMvc.perform(post("/books/" + b.getId() + "/copies")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .with(csrf())
                        .param("libraryId", lib.getId().toString())
                        .param("shelfLocation", "A-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        assertThat(copyRepository.count()).isEqualTo(before);
    }
}
