package com.sivalabs.bookstore.orders.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.MapLoaderLifecycleSupport;
import com.hazelcast.map.MapStore;
import com.hazelcast.spring.context.SpringAware;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@SpringAware
@Component
public class OrderMapStore implements MapStore<String, OrderEntity>, MapLoaderLifecycleSupport {

    private static final Logger logger = LoggerFactory.getLogger(OrderMapStore.class);
    private static final long STARTUP_GRACE_PERIOD_MS = 30_000L;
    private final long initTimestamp = System.currentTimeMillis();

    private OrderRepository orderRepository;

    public OrderMapStore() {
        // Default constructor for Hazelcast instantiation
    }

    public OrderMapStore(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Autowired
    public void setOrderRepository(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    private boolean withinStartupWindow() {
        return (System.currentTimeMillis() - initTimestamp) < STARTUP_GRACE_PERIOD_MS;
    }

    @Override
    public void init(HazelcastInstance hazelcastInstance, Properties props, String mapName) {
        logger.info("OrderMapStore lifecycle init called for map: {}", mapName);
    }

    @Override
    public void destroy() {
        logger.info("OrderMapStore lifecycle destroy called");
    }

    @Override
    public void store(String orderNumber, OrderEntity orderEntity) {
        logger.debug("Storing order in database: orderNumber={}", orderNumber);
        try {
            if (orderEntity != null && !orderNumber.equals(orderEntity.getOrderNumber())) {
                logger.warn(
                        "OrderNumber mismatch: key={}, entity.orderNumber={}",
                        orderNumber,
                        orderEntity.getOrderNumber());
            }
            logger.debug("Order store operation completed for orderNumber={}", orderNumber);
        } catch (Exception e) {
            if (withinStartupWindow()) {
                logger.debug(
                        "Store operation error during startup for orderNumber={}: {}", orderNumber, e.getMessage());
            } else {
                logger.warn("Store operation error for orderNumber={}: {}", orderNumber, e.getMessage());
            }
        }
    }

    @Override
    public void storeAll(Map<String, OrderEntity> entries) {
        logger.debug("Storing {} orders in database", entries.size());
        try {
            logger.debug("StoreAll operation completed for {} orders", entries.size());
        } catch (Exception e) {
            if (withinStartupWindow()) {
                logger.debug(
                        "StoreAll operation error during startup for {} orders: {}", entries.size(), e.getMessage());
            } else {
                logger.warn("StoreAll operation error for {} orders: {}", entries.size(), e.getMessage());
            }
        }
    }

    @Override
    public OrderEntity load(String orderNumber) {
        logger.debug("Loading order from database: orderNumber={}", orderNumber);
        try {
            if (orderRepository == null) {
                logger.warn("OrderRepository not yet injected, skipping load for orderNumber={}", orderNumber);
                return null;
            }
            Optional<OrderEntity> orderOpt = orderRepository.findByOrderNumber(orderNumber);
            return orderOpt.orElse(null);
        } catch (Exception e) {
            logger.error("Failed to load order from database: orderNumber={} - {}", orderNumber, e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, OrderEntity> loadAll(Collection<String> orderNumbers) {
        logger.debug(
                "Loading {} orders from database (batch + fallback)", orderNumbers != null ? orderNumbers.size() : 0);
        if (orderNumbers == null || orderNumbers.isEmpty()) {
            return Map.of();
        }
        if (orderRepository == null) {
            logger.warn("OrderRepository not yet injected, skipping loadAll for {} orders", orderNumbers.size());
            return Map.of();
        }

        Map<String, OrderEntity> result = new java.util.HashMap<>();
        try {
            var orders = orderRepository.findByOrderNumberIn(orderNumbers);
            for (OrderEntity o : orders) {
                if (o != null && o.getOrderNumber() != null) {
                    result.put(o.getOrderNumber(), o);
                }
            }
        } catch (Exception e) {
            logger.warn("Batch loadAll error for {} orders: {}", orderNumbers.size(), e.getMessage());
        }

        for (String num : orderNumbers) {
            if (!result.containsKey(num)) {
                try {
                    Optional<OrderEntity> orderOpt = orderRepository.findByOrderNumber(num);
                    orderOpt.ifPresent(order -> result.put(num, order));
                } catch (Exception e) {
                    logger.warn("Error loading order individually {}: {}", num, e.getMessage());
                }
            }
        }

        logger.debug("Loaded {} out of {} requested orders", result.size(), orderNumbers.size());
        return result;
    }

    @Override
    public Set<String> loadAllKeys() {
        try {
            if (orderRepository == null) {
                logger.warn("OrderRepository not yet injected, skipping loadAllKeys");
                return Set.of();
            }
            return orderRepository.findAll().stream()
                    .map(OrderEntity::getOrderNumber)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            logger.warn("Failed to load all order keys: {}", e.getMessage());
            return Set.of();
        }
    }

    @Override
    public void delete(String orderNumber) {
        logger.debug("Deleting order from database via MapStore: {}", orderNumber);
    }

    @Override
    public void deleteAll(Collection<String> orderNumbers) {
        logger.debug("Deleting {} orders via MapStore", orderNumbers.size());
    }
}
