package com.readscape.catalog.book;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

    boolean existsByAuthorId(UUID authorId);

    @Override
    @EntityGraph(attributePaths = "author")
    Page<Book> findAll(Specification<Book> spec, Pageable pageable);
}
