package com.sivalabs.bookstore.orders.domain;

import com.sivalabs.bookstore.orders.InvalidOrderException;
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
public class ProductServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceClient.class);
    private static final String CATALOG_CIRCUIT_BREAKER = "catalogApi";

    private final RestClient catalogRestClient;

    public ProductServiceClient(RestClient catalogRestClient) {
        this.catalogRestClient = catalogRestClient;
    }

    public void validate(String productCode, BigDecimal price) {
        CatalogProductResponse product = fetchProduct(productCode);
        if (product.price().compareTo(price) != 0) {
            throw new InvalidOrderException("Product price mismatch");
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
