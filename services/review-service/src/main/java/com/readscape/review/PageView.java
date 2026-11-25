package com.readscape.review;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageView<T> of(Page<T> p) {
        return new PageView<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
