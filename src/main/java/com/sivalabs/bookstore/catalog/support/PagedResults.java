package com.sivalabs.bookstore.catalog.support;

import com.sivalabs.bookstore.common.models.PagedResult;
import org.springframework.data.domain.Page;

public final class PagedResults {
    private PagedResults() {}

    public static <T> PagedResult<T> fromPage(Page<T> page) {
        return new PagedResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious());
    }
}
