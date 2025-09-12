package com.line.library.book;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
  @Query(value = """
      select b.*
      from books b
      where length(:q) >= 3
        and b.search_text ilike ('%' || immutable_unaccent(lower(:q)) || '%')
        and (:pubYear is null or b.pub_year = :pubYear)
      order by similarity(b.search_text, immutable_unaccent(lower(:q))) desc,
                b.id
      """, countQuery = """
      select count(*)
      from books b
      where length(:q) >= 3
        and b.search_text ilike ('%' || immutable_unaccent(lower(:q)) || '%')
        and (:pubYear is null or b.pub_year = :pubYear)
      """, nativeQuery = true)
  Page<Book> searchFuzzyAnyPaged(@Param("q") String q,
      @Param("pubYear") Integer pubYear,
      Pageable pageable);

  List<Book> findTop20ByOrderByIdDesc();
}
