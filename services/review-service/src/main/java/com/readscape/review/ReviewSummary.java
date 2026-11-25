package com.readscape.review;

import java.util.UUID;

public record ReviewSummary(UUID bookId, long reviewCount, double averageRating) {}
