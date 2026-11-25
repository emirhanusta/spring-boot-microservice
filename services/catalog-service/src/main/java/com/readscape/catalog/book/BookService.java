package com.readscape.catalog.book;

import com.readscape.catalog.author.Author;
import com.readscape.catalog.author.AuthorRepository;
import com.readscape.catalog.common.ConflictException;
import com.readscape.catalog.common.PageView;
import com.readscape.catalog.storage.CoverStorage;
import com.readscape.common.web.NotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookService {

    private final BookRepository books;
    private final AuthorRepository authors;
    private final CoverStorage covers;

    public BookService(BookRepository books, AuthorRepository authors, CoverStorage covers) {
        this.books = books;
        this.authors = authors;
        this.covers = covers;
    }

    @Transactional(readOnly = true)
    public PageView<BookResponse> list(UUID authorId, Genre genre, String q, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("title", "id"));
        return PageView.of(books.findAll(filter(authorId, genre, q), pageable).map(BookResponse::of));
    }

    @Transactional(readOnly = true)
    public PageView<BookResponse> listByAuthor(UUID authorId, int page, int size) {
        if (!authors.existsById(authorId)) {
            throw NotFoundException.of("Author", authorId);
        }
        return list(authorId, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public BookResponse get(UUID id) {
        return BookResponse.of(find(id));
    }

    public BookResponse create(BookRequest r) {
        Book b = new Book(author(r.authorId()), r.title(), r.description(), r.publishedDate(), copies(r), genres(r));
        return BookResponse.of(books.saveAndFlush(b));
    }

    public BookResponse update(UUID id, BookRequest r) {
        Book b = find(id);
        if (r.version() != null && r.version() != b.getVersion()) {
            throw new ConflictException("Book was modified concurrently (version " + b.getVersion() + ")");
        }
        b.update(author(r.authorId()), r.title(), r.description(), r.publishedDate(), copies(r), genres(r));
        return BookResponse.of(books.saveAndFlush(b));
    }

    public void delete(UUID id) {
        books.delete(find(id));
        books.flush();
        covers.delete(CoverStorage.keyFor(id));
    }

    public Book find(UUID id) {
        return books.findById(id).orElseThrow(() -> NotFoundException.of("Book", id));
    }

    private Author author(UUID id) {
        return authors.findById(id).orElseThrow(() -> NotFoundException.of("Author", id));
    }

    private static int copies(BookRequest r) {
        return r.totalCopies() == null ? 1 : r.totalCopies();
    }

    private static Set<Genre> genres(BookRequest r) {
        return r.genres() == null || r.genres().isEmpty() ? EnumSet.noneOf(Genre.class) : EnumSet.copyOf(r.genres());
    }

    private static Specification<Book> filter(UUID authorId, Genre genre, String q) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (authorId != null) {
                ps.add(cb.equal(root.get("author").get("id"), authorId));
            }
            if (genre != null) {
                ps.add(cb.isMember(genre, root.<Set<Genre>>get("genres")));
            }
            if (q != null && !q.isBlank()) {
                String escaped = q.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                ps.add(cb.like(cb.lower(root.get("title")), "%" + escaped + "%", '\\'));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }
}
