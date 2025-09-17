package com.sivalabs.bookstore.orders.domain;

import com.sivalabs.bookstore.orders.api.events.OrderCreatedEvent;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Caching(
            put = {@CachePut(value = "orders", key = "#result.orderNumber")},
            evict = {@CacheEvict(value = "ordersList", allEntries = true)})
    public OrderEntity createOrder(OrderEntity orderEntity) {
        if (orderEntity.getOrderNumber() == null || orderEntity.getOrderNumber().isBlank()) {
            orderEntity.setOrderNumber(UUID.randomUUID().toString());
        }

        if (orderEntity.getStatus() == null) {
            orderEntity.setStatus(OrderStatus.NEW);
        }

        OrderEntity savedOrder = orderRepository.save(orderEntity);
        log.info("Created Order with orderNumber={}", savedOrder.getOrderNumber());

        // Publish event after transaction commits
        publishOrderCreatedEvent(savedOrder);

        return savedOrder;
    }

    private void publishOrderCreatedEvent(OrderEntity savedOrder) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getOrderNumber(),
                savedOrder.getOrderItem().code(),
                savedOrder.getOrderItem().quantity(),
                savedOrder.getCustomer());

        // Register event to be published after transaction commits
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(event);
                    log.debug("Published OrderCreatedEvent for order: {}", savedOrder.getOrderNumber());
                }
            });
        } else {
            // If no transaction is active, publish immediately (for testing scenarios)
            eventPublisher.publishEvent(event);
            log.debug("Published OrderCreatedEvent immediately for order: {}", savedOrder.getOrderNumber());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#orderNumber", unless = "#result == null")
    public Optional<OrderEntity> findOrder(String orderNumber) {
        log.debug("Fetching order from database: {}", orderNumber);
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "ordersList", key = "'all'", unless = "#result.isEmpty()")
    public List<OrderEntity> findOrders() {
        Sort sort = Sort.by("id").descending();
        return orderRepository.findAllBy(sort);
    }

    @Transactional(readOnly = true)
    public Page<OrderEntity> findOrders(Pageable pageable) {
        return orderRepository.findAllWithItems(pageable);
    }

    @Transactional(readOnly = true)
    public Page<OrderEntity> findOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<OrderEntity> findOrdersByCustomerEmail(String email, Pageable pageable) {
        return orderRepository.findByCustomerEmail(email, pageable);
    }

    @Transactional
    @Caching(
            put = {@CachePut(value = "orders", key = "#orderEntity.orderNumber")},
            evict = {@CacheEvict(value = "ordersList", allEntries = true)})
    public OrderEntity updateOrderStatus(String orderNumber, OrderStatus newStatus) {
        OrderEntity order = orderRepository
                .findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        OrderEntity updatedOrder = orderRepository.save(order);

        log.info("Updated order {} status from {} to {}", orderNumber, oldStatus, newStatus);
        return updatedOrder;
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(value = "orders", key = "#orderNumber"),
                @CacheEvict(value = "ordersList", allEntries = true)
            })
    public void cancelOrder(String orderNumber) {
        OrderEntity order = orderRepository
                .findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));

        if (!canBeCancelled(order.getStatus())) {
            throw new IllegalStateException(
                    "Order " + orderNumber + " cannot be cancelled in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Cancelled order: {}", orderNumber);
    }

    private boolean canBeCancelled(OrderStatus status) {
        return status == OrderStatus.NEW || status == OrderStatus.PENDING;
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) {
            super(message);
        }
    }
}
