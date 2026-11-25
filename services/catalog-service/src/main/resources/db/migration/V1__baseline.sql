CREATE TABLE author (
    id         UUID PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    biography  TEXT,
    birth_date DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version    BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE book (
    id             UUID PRIMARY KEY,
    author_id      UUID NOT NULL REFERENCES author (id),
    title          VARCHAR(300) NOT NULL,
    description    TEXT,
    published_date DATE,
    total_copies   INT NOT NULL DEFAULT 1 CHECK (total_copies >= 0),
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    version        BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_book_author ON book (author_id);

CREATE TABLE book_genre (
    book_id UUID NOT NULL REFERENCES book (id) ON DELETE CASCADE,
    genre   VARCHAR(40) NOT NULL,
    PRIMARY KEY (book_id, genre)
);
