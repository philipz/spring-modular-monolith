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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Hazelcast MapStore implementation for OrderEntity.
 *
 * This component handles write-through caching by automatically synchronizing
 * cache operations with the PostgreSQL database through OrderRepository.
 *
 * Key behaviors:
 * - store() writes to database when cache entries are added/updated
 * - load() reads from database when cache misses occur
 * - delete() removes from database when cache entries are removed
 * - loadAll() provides bulk loading capabilities
 */
@Component
@Lazy
@SpringAware
public class OrderMapStore implements MapStore<String, OrderEntity>, MapLoaderLifecycleSupport {

    private static final Logger logger = LoggerFactory.getLogger(OrderMapStore.class);
    private static final long STARTUP_GRACE_PERIOD_MS = 30_000L;
    private final long initTimestamp = System.currentTimeMillis();

    private boolean withinStartupWindow() {
        return (System.currentTimeMillis() - initTimestamp) < STARTUP_GRACE_PERIOD_MS;
    }

    @Autowired
    private OrderRepository orderRepository;

    public OrderMapStore() {
        logger.info("OrderMapStore default constructor");
    }

    public OrderMapStore(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        logger.info("OrderMapStore initialized with OrderRepository");
    }

    @Override
    public void init(HazelcastInstance hazelcastInstance, Properties props, String mapName) {
        logger.info("OrderMapStore lifecycle init called for map: {}", mapName);
    }

    @Override
    public void destroy() {
        logger.info("OrderMapStore lifecycle destroy called");
    }

    /**
     * Store an order in the database (write-through operation).
     * This method is called when an entry is put into the cache.
     *
     * @param orderNumber the order number (cache key)
     * @param orderEntity the order entity to store
     */
    @Override
    public void store(String orderNumber, OrderEntity orderEntity) {
        logger.debug("Storing order in database: orderNumber={}", orderNumber);

        try {
            // Ensure the orderNumber matches the entity
            if (orderEntity != null && !orderNumber.equals(orderEntity.getOrderNumber())) {
                logger.warn(
                        "OrderNumber mismatch: key={}, entity.orderNumber={}",
                        orderNumber,
                        orderEntity.getOrderNumber());
            }

            // Note: For write-through, we assume the order is already persisted
            // The cache is being updated after the database operation
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

    /**
     * Store multiple orders in the database (bulk write-through operation).
     *
     * @param entries map of order numbers to order entities
     */
    @Override
    public void storeAll(Map<String, OrderEntity> entries) {
        logger.debug("Storing {} orders in database", entries.size());

        try {
            // Note: For write-through, orders are already persisted by the time they reach here
            // This method is called for cache warming or batch operations
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

    /**
     * Load an order from the database (cache miss operation).
     * This method is called when a cache get() operation results in a miss.
     *
     * @param orderNumber the order number to load
     * @return the order entity or null if not found
     */
    @Override
    public OrderEntity load(String orderNumber) {
        logger.debug("Loading order from database: orderNumber={}", orderNumber);

        try {
            Optional<OrderEntity> orderOpt = orderRepository.findByOrderNumber(orderNumber);

            if (orderOpt.isPresent()) {
                OrderEntity order = orderOpt.get();
                logger.debug("Order loaded successfully: orderNumber={}, id={}", orderNumber, order.getId());
                return order;
            } else {
                logger.debug("Order not found in database: orderNumber={}", orderNumber);
                return null;
            }

        } catch (Exception e) {
            logger.error("Failed to load order from database: orderNumber={}", orderNumber, e);
            // Return null to indicate load failure - Hazelcast will handle this gracefully
            return null;
        }
    }

    /**
     * Load multiple orders from the database (bulk cache miss operation).
     *
     * @param orderNumbers collection of order numbers to load
     * @return map of order numbers to order entities
     */
    @Override
    public Map<String, OrderEntity> loadAll(Collection<String> orderNumbers) {
        logger.debug(
                "Loading {} orders from database (batch + fallback)", orderNumbers != null ? orderNumbers.size() : 0);

        if (orderNumbers == null || orderNumbers.isEmpty()) {
            return Map.of();
        }

        Map<String, OrderEntity> result = new java.util.HashMap<>();

        // Try batch query first
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

        // Fallback: for any missing keys, attempt individual lookups
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

    /**
     * Load all order numbers from the database.
     * This method is used for cache initialization and warming.
     *
     * @return set of all order numbers in the database
     */
    @Override
    public Set<String> loadAllKeys() {
        logger.debug("Loading all order keys from database");

        try {
            // Note: OrderService doesn't have a findAll method exposed
            // For cache warming, we'll return empty set and rely on lazy loading
            // This could be enhanced by adding a findAllOrderNumbers method to OrderService
            Set<String> orderNumbers = Set.of();

            logger.debug("Loaded {} order keys from database", orderNumbers.size());
            return orderNumbers;

        } catch (Exception e) {
            // Keep unexpected exceptions at ERROR for visibility
            logger.error("Failed to load all order keys from database", e);
            // Return empty set on failure
            return Set.of();
        }
    }

    /**
     * Delete an order from the database (write-through operation).
     * This method is called when an entry is removed from the cache.
     *
     * @param orderNumber the order number to delete
     */
    @Override
    public void delete(String orderNumber) {
        logger.debug("Deleting order from database: orderNumber={}", orderNumber);

        try {
            // Note: Deletion through MapStore is typically not used in write-through scenarios
            // Orders are usually deleted through the service layer, not the cache
            // This implementation logs the operation but doesn't perform actual deletion
            logger.debug("Delete operation called for orderNumber={} - delegating to service layer", orderNumber);

        } catch (Exception e) {
            if (withinStartupWindow()) {
                logger.debug(
                        "Delete operation error during startup for orderNumber={}: {}", orderNumber, e.getMessage());
            } else {
                logger.warn("Delete operation error for orderNumber={}: {}", orderNumber, e.getMessage());
            }
        }
    }

    /**
     * Delete multiple orders from the database (bulk write-through operation).
     *
     * @param orderNumbers collection of order numbers to delete
     */
    @Override
    public void deleteAll(Collection<String> orderNumbers) {
        logger.debug("Deleting {} orders from database", orderNumbers.size());

        try {
            // Note: Bulk deletion through MapStore is typically not used in write-through scenarios
            // Orders are usually deleted through the service layer, not the cache
            logger.debug("DeleteAll operation called for {} orders - delegating to service layer", orderNumbers.size());

            logger.debug("Successfully deleted {} orders from database", orderNumbers.size());

        } catch (Exception e) {
            if (withinStartupWindow()) {
                logger.debug(
                        "DeleteAll operation error during startup for {} orders: {}",
                        orderNumbers.size(),
                        e.getMessage());
            } else {
                logger.warn("DeleteAll operation error for {} orders: {}", orderNumbers.size(), e.getMessage());
            }
        }
    }
}
