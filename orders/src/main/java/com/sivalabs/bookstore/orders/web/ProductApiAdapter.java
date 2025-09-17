package com.sivalabs.bookstore.orders.web;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter to provide ProductApi-like interface using HTTP client to catalog service.
 * This allows CartController to work with external catalog service.
 */
@Component
public class ProductApiAdapter {
    private static final Logger log = LoggerFactory.getLogger(ProductApiAdapter.class);
    private static final String CATALOG_CIRCUIT_BREAKER = "catalogApi";

    private final RestClient productRestClient;

    public ProductApiAdapter(RestClient productRestClient) {
        this.productRestClient = productRestClient;
    }

    /**
     * Get product by code, compatible with ProductApi.getByCode()
     */
    public Optional<ProductDto> getByCode(String code) {
        try {
            ProductDto product = fetchProduct(code);
            return Optional.of(product);
        } catch (ProductNotFoundException e) {
            log.debug("Product not found with code: {}", code);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error fetching product with code: {}, error: {}", code, e.getMessage());
            return Optional.empty();
        }
    }

    @CircuitBreaker(name = CATALOG_CIRCUIT_BREAKER, fallbackMethod = "handleFetchFailure")
    @Retry(name = CATALOG_CIRCUIT_BREAKER)
    ProductDto fetchProduct(String productCode) {
        return productRestClient
                .get()
                .uri("/api/products/{code}", productCode)
                .retrieve()
                .onStatus(status -> isNotFound(status), (request, response) -> {
                    throw new ProductNotFoundException("Product not found with code: " + productCode);
                })
                .body(ProductDto.class);
    }

    ProductDto handleFetchFailure(String productCode, Throwable throwable) {
        log.error("Catalog API unavailable for product {}: {}", productCode, throwable.getMessage());
        throw new CatalogServiceException("Unable to fetch product details from catalog service", throwable);
    }

    private boolean isNotFound(HttpStatusCode status) {
        return status.value() == HttpStatus.NOT_FOUND.value();
    }

    /**
     * Product DTO compatible with catalog service response
     */
    public record ProductDto(String code, String name, String description, String imageUrl, BigDecimal price) {
        public String getDisplayName() {
            if (name.length() <= 20) {
                return name;
            }
            return name.substring(0, 20) + "...";
        }
    }

    /**
     * Exception for product not found cases
     */
    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception for catalog service communication failures
     */
    public static class CatalogServiceException extends RuntimeException {
        public CatalogServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
