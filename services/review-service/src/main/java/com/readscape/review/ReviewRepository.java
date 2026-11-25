package com.readscape.review;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("""
            select r from Review r
            where (:bookId is null or r.bookId = :bookId) and (:userId is null or r.userId = :userId)""")
    Page<Review> search(@Param("bookId") UUID bookId, @Param("userId") UUID userId, Pageable pageable);

    boolean existsByBookIdAndUserId(UUID bookId, UUID userId);

    long countByBookId(UUID bookId);

    @Query("select avg(r.rating) from Review r where r.bookId = :bookId")
    Double averageRating(@Param("bookId") UUID bookId);
}
