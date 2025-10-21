package com.sivalabs.bookstore.catalog.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Product information")
public record ProductDto(
        @Schema(description = "Unique product code", example = "P100", required = true) String code,
        @Schema(description = "Product name", example = "The Hunger Games", required = true) String name,
        @Schema(
                        description = "Product description",
                        example = "Winning will make you famous. Losing means certain death...")
                String description,
        @Schema(description = "Product image URL", example = "https://images.sivalabs.in/products/the-hunger-games.jpg")
                String imageUrl,
        @Schema(description = "Product price", example = "34.0", required = true) BigDecimal price) {
    @JsonIgnore
    public String getDisplayName() {
        if (name.length() <= 20) {
            return name;
        }
        return name.substring(0, 20) + "...";
    }
}
