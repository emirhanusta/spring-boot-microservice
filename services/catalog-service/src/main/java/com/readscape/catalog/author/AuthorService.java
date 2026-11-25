package com.readscape.catalog.author;

import com.readscape.catalog.book.BookRepository;
import com.readscape.catalog.common.ConflictException;
import com.readscape.catalog.common.PageView;
import com.readscape.common.web.NotFoundException;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthorService {

    private final AuthorRepository authors;
    private final BookRepository books;

    public AuthorService(AuthorRepository authors, BookRepository books) {
        this.authors = authors;
        this.books = books;
    }

    @Transactional(readOnly = true)
    public PageView<AuthorResponse> list(int page, int size) {
        return PageView.of(authors.findAll(PageRequest.of(page, size, Sort.by("name", "id"))).map(AuthorResponse::of));
    }

    @Transactional(readOnly = true)
    public AuthorResponse get(UUID id) {
        return AuthorResponse.of(find(id));
    }

    public AuthorResponse create(AuthorRequest r) {
        return AuthorResponse.of(authors.saveAndFlush(new Author(r.name(), r.biography(), r.birthDate())));
    }

    public AuthorResponse update(UUID id, AuthorRequest r) {
        Author a = find(id);
        if (r.version() != null && r.version() != a.getVersion()) {
            throw new ConflictException("Author was modified concurrently (version " + a.getVersion() + ")");
        }
        a.update(r.name(), r.biography(), r.birthDate());
        return AuthorResponse.of(authors.saveAndFlush(a));
    }

    public void delete(UUID id) {
        Author a = find(id);
        if (books.existsByAuthorId(id)) {
            throw new ConflictException("Author still has books; delete or reassign them first");
        }
        authors.delete(a);
        authors.flush();
    }

    Author find(UUID id) {
        return authors.findById(id).orElseThrow(() -> NotFoundException.of("Author", id));
    }
}
