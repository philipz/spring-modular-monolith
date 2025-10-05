package com.sivalabs.bookstore.orders.infrastructure.catalog;

import com.sivalabs.bookstore.common.grpc.ProductCatalogServiceGrpc;
import com.sivalabs.bookstore.common.grpc.ProductValidationRequest;
import com.sivalabs.bookstore.common.grpc.ProductValidationResponse;
import com.sivalabs.bookstore.orders.InvalidOrderException;
import com.sivalabs.bookstore.orders.domain.ProductCatalogPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * gRPC-based implementation of {@link ProductCatalogPort} for product validation.
 *
 * <p>This client communicates with the Product Catalog service via gRPC to validate
 * product codes and prices during order creation. It maintains the same validation
 * logic as {@link HttpProductCatalogClient} including price tolerance handling for
 * floating-point precision issues.</p>
 *
 * <p>Features resilience patterns with circuit breaker and retry mechanisms to handle
 * service failures gracefully.</p>
 */
@Component
@ConditionalOnProperty(name = "bookstore.catalog.client.type", havingValue = "grpc")
public class GrpcProductCatalogClient implements ProductCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(GrpcProductCatalogClient.class);
    private static final String CATALOG_CIRCUIT_BREAKER = "catalogApi";

    private final ProductCatalogServiceGrpc.ProductCatalogServiceBlockingStub catalogStub;

    public GrpcProductCatalogClient(ProductCatalogServiceGrpc.ProductCatalogServiceBlockingStub catalogStub) {
        this.catalogStub = catalogStub;
    }

    @Override
    public void validate(String productCode, BigDecimal price) {
        log.info("🔍 [gRPC-CLIENT] Starting product validation for code={}, price={}", productCode, price);

        ProductValidationResponse response = validateProduct(productCode, price);

        if (!response.getValid()) {
            log.warn("❌ [gRPC-CLIENT] Product validation failed for code={}: {}", productCode, response.getMessage());
            throw new InvalidOrderException(response.getMessage());
        }

        // Validate price with tolerance for floating-point precision issues
        // Convert gRPC double back to BigDecimal for accurate comparison
        BigDecimal productPrice = BigDecimal.valueOf(response.getProduct().getPrice());
        BigDecimal tolerance = new BigDecimal("0.01"); // 1 cent tolerance
        BigDecimal difference = productPrice.subtract(price).abs();

        if (difference.compareTo(tolerance) > 0) {
            log.warn(
                    "❌ [gRPC-CLIENT] Price validation failed for code={}. Expected: {}, Provided: {}, Difference: {}",
                    productCode,
                    productPrice,
                    price,
                    difference);
            throw new InvalidOrderException(String.format(
                    "Product price mismatch for %s. Expected: %s, Provided: %s", productCode, productPrice, price));
        }

        log.info(
                "✅ [gRPC-CLIENT] Product validation successful for code={}, productName={}, catalogPrice={}",
                productCode,
                response.getProduct().getName(),
                productPrice);
    }

    @CircuitBreaker(name = CATALOG_CIRCUIT_BREAKER, fallbackMethod = "handleValidationFailure")
    @Retry(name = CATALOG_CIRCUIT_BREAKER)
    ProductValidationResponse validateProduct(String productCode, BigDecimal price) {
        try {
            log.debug("📤 [gRPC-CLIENT] Preparing gRPC request for catalog validation");

            ProductValidationRequest request = ProductValidationRequest.newBuilder()
                    .setProductCode(productCode)
                    .setPrice(price.doubleValue())
                    .build();

            log.info("🚀 [gRPC-CLIENT] Invoking ProductCatalogService.validateProduct for code={}", productCode);

            ProductValidationResponse response = catalogStub.validateProduct(request);

            log.info(
                    "📥 [gRPC-CLIENT] Received gRPC response: valid={}, message={}",
                    response.getValid(),
                    response.getMessage());

            return response;
        } catch (StatusRuntimeException e) {
            log.error(
                    "🚨 [gRPC-CLIENT] gRPC call failed for code={}: status={}, description={}",
                    productCode,
                    e.getStatus().getCode(),
                    e.getStatus().getDescription());
            handleGrpcException(productCode, e);
            throw new CatalogServiceException("Unexpected gRPC error", e);
        }
    }

    ProductValidationResponse handleValidationFailure(String productCode, BigDecimal price, Throwable throwable) {
        log.error(
                "🔄 [gRPC-CLIENT] Circuit breaker fallback triggered for product {}: {}",
                productCode,
                throwable.getMessage());
        throw new CatalogServiceException("Unable to validate product via catalog gRPC service", throwable);
    }

    private void handleGrpcException(String productCode, StatusRuntimeException e) {
        Status.Code code = e.getStatus().getCode();
        String message = e.getStatus().getDescription();

        switch (code) {
            case NOT_FOUND:
                throw new InvalidOrderException("Product not found with code: " + productCode);
            case INVALID_ARGUMENT:
                throw new InvalidOrderException("Invalid product validation request: " + message);
            case UNAVAILABLE:
            case DEADLINE_EXCEEDED:
                log.warn("Catalog service unavailable for product {}: {}", productCode, message);
                throw new CatalogServiceException("Catalog service temporarily unavailable", e);
            default:
                log.error("Unexpected gRPC error for product {}: {}", productCode, message);
                throw new CatalogServiceException("Unexpected catalog service error", e);
        }
    }
}
