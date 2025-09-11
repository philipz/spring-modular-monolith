package com.sivalabs.bookstore.orders.web;

import com.sivalabs.bookstore.orders.CreateOrderRequest;
import com.sivalabs.bookstore.orders.CreateOrderResponse;
import com.sivalabs.bookstore.orders.OrderDto;
import com.sivalabs.bookstore.orders.OrderNotFoundException;
import com.sivalabs.bookstore.orders.OrderView;
import com.sivalabs.bookstore.orders.cache.OrderCacheService;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import com.sivalabs.bookstore.orders.domain.OrderService;
import com.sivalabs.bookstore.orders.domain.ProductServiceClient;
import com.sivalabs.bookstore.orders.mappers.OrderMapper;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
class OrderRestController {
    private static final Logger log = LoggerFactory.getLogger(OrderRestController.class);

    private final OrderService orderService;
    private final ProductServiceClient productServiceClient;
    private final OrderCacheService orderCacheService;

    OrderRestController(
            OrderService orderService,
            ProductServiceClient productServiceClient,
            Optional<OrderCacheService> orderCacheService) {
        this.orderService = orderService;
        this.productServiceClient = productServiceClient;
        this.orderCacheService = orderCacheService.orElse(null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        productServiceClient.validate(request.item().code(), request.item().price());
        OrderEntity newOrder = OrderMapper.convertToEntity(request);
        var savedOrder = orderService.createOrder(newOrder);
        return new CreateOrderResponse(savedOrder.getOrderNumber());
    }

    @GetMapping(value = "/{orderNumber}")
    OrderDto getOrder(@PathVariable String orderNumber) {
        log.info("Fetching order by orderNumber: {}", orderNumber);
        return orderService
                .findOrder(orderNumber)
                .map(OrderMapper::convertToDto)
                .orElseThrow(() -> OrderNotFoundException.forOrderNumber(orderNumber));
    }

    @GetMapping
    List<OrderView> getOrders() {
        List<OrderEntity> orders = orderService.findOrders();
        return OrderMapper.convertToOrderViews(orders);
    }

    @GetMapping("/cache/info")
    ResponseEntity<Map<String, Object>> getCacheInfo() {
        Map<String, Object> cacheInfo = new HashMap<>();

        if (orderCacheService == null) {
            cacheInfo.put("status", "disabled");
            cacheInfo.put("message", "Hazelcast cache is not enabled or not available");
            return ResponseEntity.ok(cacheInfo);
        }

        try {
            cacheInfo.put("status", "enabled");
            cacheInfo.put("healthy", orderCacheService.isHealthy());
            cacheInfo.put("circuitBreakerOpen", orderCacheService.isCircuitBreakerOpen());
            cacheInfo.put("statistics", orderCacheService.getCacheStats());
            cacheInfo.put("circuitBreakerStatus", orderCacheService.getCircuitBreakerStatus());
            cacheInfo.put("healthReport", orderCacheService.getHealthReport());

            return ResponseEntity.ok(cacheInfo);
        } catch (Exception e) {
            log.error("Failed to retrieve cache information", e);
            cacheInfo.put("status", "error");
            cacheInfo.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cacheInfo);
        }
    }

    @GetMapping("/cache/health")
    ResponseEntity<Map<String, Object>> getCacheHealth() {
        Map<String, Object> healthInfo = new HashMap<>();

        if (orderCacheService == null) {
            healthInfo.put("status", "DOWN");
            healthInfo.put("details", "Cache service is not available");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(healthInfo);
        }

        try {
            boolean isHealthy = orderCacheService.isHealthy();
            boolean circuitBreakerOpen = orderCacheService.isCircuitBreakerOpen();

            healthInfo.put("status", isHealthy && !circuitBreakerOpen ? "UP" : "DOWN");
            healthInfo.put("healthy", isHealthy);
            healthInfo.put("circuitBreakerOpen", circuitBreakerOpen);
            healthInfo.put("connectivity", orderCacheService.testCacheConnectivity());

            if (!isHealthy || circuitBreakerOpen) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(healthInfo);
            }

            return ResponseEntity.ok(healthInfo);
        } catch (Exception e) {
            log.error("Failed to check cache health", e);
            healthInfo.put("status", "DOWN");
            healthInfo.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(healthInfo);
        }
    }

    @GetMapping("/cache/stats")
    ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> statsInfo = new HashMap<>();

        if (orderCacheService == null) {
            statsInfo.put("available", false);
            statsInfo.put("message", "Cache service is not available");
            return ResponseEntity.ok(statsInfo);
        }

        try {
            statsInfo.put("available", true);
            statsInfo.put("statistics", orderCacheService.getCacheStats());
            statsInfo.put("circuitBreakerStatus", orderCacheService.getCircuitBreakerStatus());

            return ResponseEntity.ok(statsInfo);
        } catch (Exception e) {
            log.error("Failed to retrieve cache statistics", e);
            statsInfo.put("available", false);
            statsInfo.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(statsInfo);
        }
    }

    @PostMapping("/cache/test-connectivity")
    ResponseEntity<Map<String, Object>> testCacheConnectivity() {
        Map<String, Object> testResult = new HashMap<>();

        if (orderCacheService == null) {
            testResult.put("success", false);
            testResult.put("message", "Cache service is not available");
            return ResponseEntity.ok(testResult);
        }

        try {
            boolean connectivityTest = orderCacheService.testCacheConnectivity();
            testResult.put("success", connectivityTest);
            testResult.put(
                    "message", connectivityTest ? "Cache connectivity test passed" : "Cache connectivity test failed");
            testResult.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(testResult);
        } catch (Exception e) {
            log.error("Cache connectivity test failed with exception", e);
            testResult.put("success", false);
            testResult.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(testResult);
        }
    }

    @PostMapping("/cache/reset-circuit-breaker")
    ResponseEntity<Map<String, Object>> resetCircuitBreaker() {
        Map<String, Object> resetResult = new HashMap<>();

        if (orderCacheService == null) {
            resetResult.put("success", false);
            resetResult.put("message", "Cache service is not available");
            return ResponseEntity.ok(resetResult);
        }

        try {
            boolean resetSuccess = orderCacheService.resetCircuitBreaker();
            resetResult.put("success", resetSuccess);
            resetResult.put(
                    "message", resetSuccess ? "Circuit breaker reset successfully" : "Circuit breaker reset failed");
            resetResult.put("timestamp", System.currentTimeMillis());

            log.warn("Cache circuit breaker manually reset via REST API");

            return ResponseEntity.ok(resetResult);
        } catch (Exception e) {
            log.error("Failed to reset circuit breaker", e);
            resetResult.put("success", false);
            resetResult.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resetResult);
        }
    }

    @GetMapping("/cache/data")
    ResponseEntity<Map<String, Object>> getCacheData() {
        Map<String, Object> cacheData = new HashMap<>();

        if (orderCacheService == null) {
            cacheData.put("available", false);
            cacheData.put("message", "Cache service is not available");
            return ResponseEntity.ok(cacheData);
        }

        try {
            Map<String, Object> summary = orderCacheService.getCacheContentSummary();
            cacheData.put("available", true);
            cacheData.put("summary", summary);

            return ResponseEntity.ok(cacheData);
        } catch (Exception e) {
            log.error("Failed to retrieve cache data", e);
            cacheData.put("available", false);
            cacheData.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cacheData);
        }
    }

    @GetMapping("/cache/orders")
    ResponseEntity<Map<String, Object>> getAllCachedOrders() {
        Map<String, Object> response = new HashMap<>();

        if (orderCacheService == null) {
            response.put("available", false);
            response.put("message", "Cache service is not available");
            response.put("orders", List.of());
            return ResponseEntity.ok(response);
        }

        try {
            List<OrderEntity> cachedOrders = orderCacheService.getAllCachedOrders();
            List<OrderDto> orderDtos =
                    cachedOrders.stream().map(OrderMapper::convertToDto).toList();

            response.put("available", true);
            response.put("count", orderDtos.size());
            response.put("orders", orderDtos);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve cached orders", e);
            response.put("available", false);
            response.put("error", e.getMessage());
            response.put("orders", List.of());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/cache/orders/keys")
    ResponseEntity<Map<String, Object>> getCacheKeys() {
        Map<String, Object> response = new HashMap<>();

        if (orderCacheService == null) {
            response.put("available", false);
            response.put("message", "Cache service is not available");
            response.put("keys", Set.of());
            return ResponseEntity.ok(response);
        }

        try {
            Set<String> keys = orderCacheService.getAllCacheKeys();

            response.put("available", true);
            response.put("count", keys.size());
            response.put("keys", keys);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve cache keys", e);
            response.put("available", false);
            response.put("error", e.getMessage());
            response.put("keys", Set.of());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/cache/orders/paginated")
    ResponseEntity<Map<String, Object>> getCachedOrdersPaginated(
            @RequestParam(defaultValue = "10") int limit, @RequestParam(defaultValue = "0") int offset) {
        Map<String, Object> response = new HashMap<>();

        if (orderCacheService == null) {
            response.put("available", false);
            response.put("message", "Cache service is not available");
            response.put("orders", List.of());
            return ResponseEntity.ok(response);
        }

        try {
            List<OrderEntity> cachedOrders = orderCacheService.getCachedOrdersPaginated(limit, offset);
            List<OrderDto> orderDtos =
                    cachedOrders.stream().map(OrderMapper::convertToDto).toList();

            int totalCachedOrders = orderCacheService.getAllCachedOrders().size();

            response.put("available", true);
            response.put("orders", orderDtos);
            response.put(
                    "pagination",
                    Map.of(
                            "limit", limit,
                            "offset", offset,
                            "count", orderDtos.size(),
                            "total", totalCachedOrders,
                            "hasMore", offset + limit < totalCachedOrders));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve paginated cached orders", e);
            response.put("available", false);
            response.put("error", e.getMessage());
            response.put("orders", List.of());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/cache/orders/{orderNumber}")
    ResponseEntity<Map<String, Object>> getCachedOrder(@PathVariable String orderNumber) {
        Map<String, Object> response = new HashMap<>();

        if (orderCacheService == null) {
            response.put("available", false);
            response.put("message", "Cache service is not available");
            return ResponseEntity.ok(response);
        }

        try {
            Optional<OrderEntity> cachedOrder = orderCacheService.findByOrderNumber(orderNumber);

            response.put("available", true);
            response.put("found", cachedOrder.isPresent());

            if (cachedOrder.isPresent()) {
                OrderDto orderDto = OrderMapper.convertToDto(cachedOrder.get());
                response.put("order", orderDto);
            } else {
                response.put("message", "Order not found in cache");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve cached order: {}", orderNumber, e);
            response.put("available", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
