package com.sivalabs.bookstore.orders.domain;

import java.util.Optional;

public interface OrderCachePort {

    boolean cacheOrder(String orderNumber, OrderEntity order);

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    boolean isCircuitBreakerOpen();

    static OrderCachePort noop() {
        return new OrderCachePort() {
            @Override
            public boolean cacheOrder(String orderNumber, OrderEntity order) {
                return false;
            }

            @Override
            public Optional<OrderEntity> findByOrderNumber(String orderNumber) {
                return Optional.empty();
            }

            @Override
            public boolean isCircuitBreakerOpen() {
                return true;
            }
        };
    }
}
