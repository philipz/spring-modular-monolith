package com.sivalabs.bookstore.orders.domain;

import com.sivalabs.bookstore.common.models.PagedResult;
import com.sivalabs.bookstore.orders.api.events.OrderCreatedEvent;
import com.sivalabs.bookstore.orders.cache.OrderCacheService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderCacheService orderCacheService;
    private final EntityManager entityManager;

    OrderService(
            OrderRepository orderRepository,
            ApplicationEventPublisher publisher,
            @Autowired(required = false) OrderCacheService orderCacheService,
            EntityManager entityManager) {
        this.orderRepository = orderRepository;
        this.eventPublisher = publisher;
        this.orderCacheService = orderCacheService;
        this.entityManager = entityManager;

        if (orderCacheService != null) {
            log.info("OrderService initialized with cache support enabled");
        } else {
            log.info("OrderService initialized without cache support (cache disabled or unavailable)");
        }
    }

    /**
     * Check if cache service is available and operational.
     *
     * @return true if cache service is available, false otherwise
     */
    private boolean isCacheAvailable() {
        return orderCacheService != null && !orderCacheService.isCircuitBreakerOpen();
    }

    @Transactional
    public OrderEntity createOrder(OrderEntity orderEntity) {
        OrderEntity savedOrder = orderRepository.save(orderEntity);
        log.info("Created Order with orderNumber={}", savedOrder.getOrderNumber());

        // Cache the newly created order if cache service is available
        if (isCacheAvailable()) {
            try {
                orderCacheService.cacheOrder(savedOrder.getOrderNumber(), savedOrder);
                log.debug("Order cached successfully: {}", savedOrder.getOrderNumber());
            } catch (Exception e) {
                log.warn(
                        "Failed to cache order {} - order creation will continue: {}",
                        savedOrder.getOrderNumber(),
                        e.getMessage());
                // Continue with order creation - cache failure should not break business logic
            }
        } else {
            log.debug(
                    "Cache service unavailable - skipping cache operation for order: {}", savedOrder.getOrderNumber());
        }

        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getOrderNumber(),
                savedOrder.getOrderItem().code(),
                savedOrder.getOrderItem().quantity(),
                savedOrder.getCustomer());
        eventPublisher.publishEvent(event);
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Optional<OrderEntity> findOrder(String orderNumber) {
        // Try cache first if available
        if (isCacheAvailable()) {
            try {
                Optional<OrderEntity> cachedOrder = orderCacheService.findByOrderNumber(orderNumber);
                if (cachedOrder.isPresent()) {
                    log.debug("Order found in cache: {}", orderNumber);
                    return cachedOrder;
                }
                log.debug("Cache miss for order: {}", orderNumber);
            } catch (Exception e) {
                log.warn(
                        "Failed to read from cache for order {} - falling back to database: {}",
                        orderNumber,
                        e.getMessage());
            }
        } else {
            log.debug("Cache service unavailable - querying database directly for order: {}", orderNumber);
        }

        // Cache miss or cache unavailable - query database
        Optional<OrderEntity> order = orderRepository.findByOrderNumber(orderNumber);

        // Cache the result if found and cache is available
        if (order.isPresent() && isCacheAvailable()) {
            try {
                orderCacheService.cacheOrder(orderNumber, order.get());
                log.debug("Order cached after database retrieval: {}", orderNumber);
            } catch (Exception e) {
                log.warn("Failed to cache order {} after database retrieval: {}", orderNumber, e.getMessage());
                // Continue with returning the order - cache failure should not affect read operation
            }
        }

        return order;
    }

    @Transactional(readOnly = true)
    public PagedResult<OrderEntity> findOrders(int page, int size) {
        int validPage = Math.max(page, 1);
        int validSize = Math.max(size, 1);

        TypedQuery<Long> countQuery = entityManager.createQuery("select count(o) from OrderEntity o", Long.class);
        long total = countQuery.getSingleResult();

        TypedQuery<OrderEntity> dataQuery = entityManager
                .createQuery("select o from OrderEntity o order by o.id desc", OrderEntity.class)
                .setFirstResult((validPage - 1) * validSize)
                .setMaxResults(validSize);
        List<OrderEntity> orders = dataQuery.getResultList();

        long totalPagesLong = total == 0 ? 0 : (long) Math.ceil((double) total / validSize);
        int totalPages = (int) Math.max(0, totalPagesLong);
        boolean isFirst = validPage <= 1 || total == 0;
        boolean isLast = total == 0 || validPage >= totalPages;
        boolean hasNext = !isLast && totalPages > 0;
        boolean hasPrevious = validPage > 1 && total > 0;

        return new PagedResult<>(orders, total, validPage, totalPages, isFirst, isLast, hasNext, hasPrevious);
    }
}
