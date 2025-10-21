package com.sivalabs.bookstore.common.models;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;

@Schema(description = "Paginated response containing data and pagination metadata")
public record PagedResult<T>(
        @Schema(description = "List of items for the current page", required = true) List<T> data,
        @Schema(description = "Total number of elements across all pages", example = "100", required = true)
                long totalElements,
        @Schema(description = "Current page number (1-based)", example = "1", required = true, minimum = "1")
                int pageNumber,
        @Schema(description = "Total number of pages", example = "10", required = true, minimum = "0") int totalPages,
        @Schema(description = "Whether this is the first page", example = "true", required = true) boolean isFirst,
        @Schema(description = "Whether this is the last page", example = "false", required = true) boolean isLast,
        @Schema(description = "Whether there is a next page available", example = "true", required = true)
                boolean hasNext,
        @Schema(description = "Whether there is a previous page available", example = "false", required = true)
                boolean hasPrevious) {

    public static <S, T> PagedResult<T> of(PagedResult<S> pagedResult, Function<S, T> mapper) {
        return new PagedResult<>(
                pagedResult.data.stream().map(mapper).toList(),
                pagedResult.totalElements,
                pagedResult.pageNumber,
                pagedResult.totalPages,
                pagedResult.isFirst,
                pagedResult.isLast,
                pagedResult.hasNext,
                pagedResult.hasPrevious);
    }
}
