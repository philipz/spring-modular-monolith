package com.sivalabs.bookstore.orders.infrastructure.catalog;

import com.sivalabs.bookstore.orders.InvalidOrderException;
import com.sivalabs.bookstore.orders.domain.ProductCatalogPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpProductCatalogClient implements ProductCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(HttpProductCatalogClient.class);
    private static final String CATALOG_CIRCUIT_BREAKER = "catalogApi";

    private final RestClient catalogRestClient;

    public HttpProductCatalogClient(RestClient catalogRestClient) {
        this.catalogRestClient = catalogRestClient;
    }

    @Override
    public void validate(String productCode, BigDecimal price) {
        CatalogProductResponse product = fetchProduct(productCode);

        // Validate price with tolerance for floating-point precision issues
        BigDecimal tolerance = new BigDecimal("0.01"); // 1 cent tolerance
        BigDecimal difference = product.price().subtract(price).abs();

        if (difference.compareTo(tolerance) > 0) {
            throw new InvalidOrderException(String.format(
                    "Product price mismatch for %s. Expected: %s, Provided: %s", productCode, product.price(), price));
        }
    }

    @CircuitBreaker(name = CATALOG_CIRCUIT_BREAKER, fallbackMethod = "handleFetchFailure")
    @Retry(name = CATALOG_CIRCUIT_BREAKER)
    CatalogProductResponse fetchProduct(String productCode) {
        return catalogRestClient
                .get()
                .uri("/api/products/{code}", productCode)
                .retrieve()
                .onStatus(status -> isNotFound(status), (request, response) -> {
                    throw new InvalidOrderException("Product not found with code: " + productCode);
                })
                .body(CatalogProductResponse.class);
    }

    CatalogProductResponse handleFetchFailure(String productCode, Throwable throwable) {
        log.error("Catalog API unavailable for product {}: {}", productCode, throwable.getMessage());
        throw new CatalogServiceException("Unable to fetch product details from catalog service", throwable);
    }

    private boolean isNotFound(HttpStatusCode status) {
        return status.value() == HttpStatus.NOT_FOUND.value();
    }

    record CatalogProductResponse(String code, String name, BigDecimal price) {}
}
