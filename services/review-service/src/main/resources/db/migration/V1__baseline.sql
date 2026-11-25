CREATE TABLE review (
    id         UUID PRIMARY KEY,
    book_id    UUID         NOT NULL,
    user_id    UUID         NOT NULL,
    username   VARCHAR(50)  NOT NULL,
    rating     SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    body       TEXT         NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version    BIGINT       NOT NULL,
    CONSTRAINT uq_review_book_user UNIQUE (book_id, user_id)
);
CREATE INDEX idx_review_user ON review (user_id);
