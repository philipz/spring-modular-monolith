package com.sivalabs.bookstore.orders.domain;

import com.sivalabs.bookstore.orders.api.events.OrderCreatedEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderCachePort orderCachePort;
    private final boolean cacheEnabled;

    public OrderService(
            OrderRepository orderRepository,
            ApplicationEventPublisher publisher,
            @Autowired(required = false) OrderCachePort orderCachePort) {
        this.orderRepository = orderRepository;
        this.eventPublisher = publisher;
        this.orderCachePort = orderCachePort != null ? orderCachePort : OrderCachePort.noop();
        this.cacheEnabled = orderCachePort != null;

        if (cacheEnabled) {
            log.info("OrderService initialized with cache support enabled");
        } else {
            log.info("OrderService initialized without cache support (cache disabled or unavailable)");
        }
    }

    private boolean isCacheAvailable() {
        return cacheEnabled && !orderCachePort.isCircuitBreakerOpen();
    }

    @Transactional
    public OrderEntity createOrder(OrderEntity orderEntity) {
        if (orderEntity.getOrderNumber() == null || orderEntity.getOrderNumber().isBlank()) {
            orderEntity.setOrderNumber(UUID.randomUUID().toString());
        }
        OrderEntity savedOrder = orderRepository.save(orderEntity);
        log.info("Created Order with orderNumber={}", savedOrder.getOrderNumber());

        if (isCacheAvailable()) {
            try {
                orderCachePort.cacheOrder(savedOrder.getOrderNumber(), savedOrder);
                log.debug("Order cached successfully: {}", savedOrder.getOrderNumber());
            } catch (Exception e) {
                log.warn(
                        "Failed to cache order {} - order creation will continue: {}",
                        savedOrder.getOrderNumber(),
                        e.getMessage());
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
        if (isCacheAvailable()) {
            try {
                Optional<OrderEntity> cachedOrder = orderCachePort.findByOrderNumber(orderNumber);
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

        Optional<OrderEntity> order = orderRepository.findByOrderNumber(orderNumber);

        if (order.isPresent() && isCacheAvailable()) {
            try {
                orderCachePort.cacheOrder(orderNumber, order.get());
                log.debug("Order cached after database retrieval: {}", orderNumber);
            } catch (Exception e) {
                log.warn("Failed to cache order {} after database retrieval: {}", orderNumber, e.getMessage());
            }
        }

        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> findOrders() {
        Sort sort = Sort.by("id").descending();
        return orderRepository.findAllBy(sort);
    }
}
