package com.sivalabs.bookstore.catalog.web;

import com.sivalabs.bookstore.catalog.api.ProductDto;
import com.sivalabs.bookstore.catalog.domain.ProductNotFoundException;
import com.sivalabs.bookstore.catalog.domain.ProductService;
import com.sivalabs.bookstore.catalog.mappers.ProductMapper;
import com.sivalabs.bookstore.common.models.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product catalog management API")
class ProductRestController {
    private static final Logger log = LoggerFactory.getLogger(ProductRestController.class);

    private final ProductService productService;
    private final ProductMapper productMapper;

    ProductRestController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @GetMapping
    @Operation(
            summary = "Get paginated products",
            description = "Retrieves a paginated list of products from the catalog")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved product list",
                        content = @Content(schema = @Schema(implementation = PagedResult.class)))
            })
    PagedResult<ProductDto> getProducts(
            @Parameter(description = "Page number (1-based)", example = "1") @RequestParam(defaultValue = "1")
                    int page) {
        log.info("Fetching products for page: {}", page);
        var pagedResult = productService.getProducts(page);
        return PagedResult.of(pagedResult, productMapper::mapToDto);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get product by code", description = "Retrieves a specific product by its unique code")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved product",
                        content = @Content(schema = @Schema(implementation = ProductDto.class))),
                @ApiResponse(responseCode = "404", description = "Product not found", content = @Content())
            })
    ProductDto getProductByCode(
            @Parameter(description = "Product code", example = "P100", required = true) @PathVariable String code) {
        log.info("Fetching product by code: {}", code);
        return productService
                .getByCode(code)
                .map(productMapper::mapToDto)
                .orElseThrow(() -> ProductNotFoundException.forCode(code));
    }
}
