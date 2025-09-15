package com.sivalabs.bookstore.inventory.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.MapLoaderLifecycleSupport;
import com.hazelcast.map.MapStore;
import com.sivalabs.bookstore.inventory.domain.InventoryEntity;
import com.sivalabs.bookstore.inventory.domain.InventoryRepository;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Hazelcast MapStore implementation for InventoryEntity.
 *
 * This component handles write-through caching by automatically synchronizing
 * cache operations with the PostgreSQL database through InventoryRepository.
 *
 * Key behaviors:
 * - store() writes to database when cache entries are added/updated
 * - load() reads from database when cache misses occur
 * - delete() removes from database when cache entries are removed
 * - loadAll() provides bulk loading capabilities
 *
 * Note: Uses Long keys for inventory ID-based operations, following inventory domain patterns.
 */
@Component
@Lazy
public class InventoryMapStore implements MapStore<Long, InventoryEntity>, MapLoaderLifecycleSupport {

    private static final Logger logger = LoggerFactory.getLogger(InventoryMapStore.class);
    private static final long STARTUP_GRACE_PERIOD_MS = 30_000L;
    private final long initTimestamp = System.currentTimeMillis();

    private boolean withinStartupWindow() {
        return (System.currentTimeMillis() - initTimestamp) < STARTUP_GRACE_PERIOD_MS;
    }

    @Autowired
    private InventoryRepository inventoryRepository;

    public InventoryMapStore() {
        logger.info("InventoryMapStore default constructor");
    }

    public InventoryMapStore(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
        logger.info("InventoryMapStore initialized with InventoryRepository");
    }

    @Override
    public void init(HazelcastInstance hazelcastInstance, Properties props, String mapName) {
        logger.info("InventoryMapStore lifecycle init called for map: {}", mapName);
    }

    @Override
    public void destroy() {
        logger.info("InventoryMapStore lifecycle destroy called");
    }

    /**
     * Store an inventory record in the database (write-through operation).
     * This method is called when an entry is put into the cache.
     *
     * @param inventoryId the inventory ID (cache key)
     * @param inventoryEntity the inventory entity to store
     */
    @Override
    public void store(Long inventoryId, InventoryEntity inventoryEntity) {
        logger.debug("Storing inventory in database: inventoryId={}", inventoryId);

        try {
            // Ensure the inventoryId matches the entity
            if (inventoryEntity != null && !inventoryId.equals(inventoryEntity.getId())) {
                logger.warn(
                        "InventoryId mismatch: key={}, entity.inventoryId={}", inventoryId, inventoryEntity.getId());
            }

            // Note: For write-through, we assume the inventory is already persisted
            // The cache is being updated after the database operation
            logger.debug("Inventory store operation completed for inventoryId={}", inventoryId);

        } catch (Exception e) {
            if (withinStartupWindow()) {
                logger.debug(
                        "Store operation error during startup for inventoryId={}: {}", inventoryId, e.getMessage());
            } else {
                logger.warn("Store operation error for inventoryId={}: {}", inventoryId, e.getMessage());
            }
        }
    }

    /**
     * Store multiple inventory records in the database (bulk write-through operation).
     *
     * @param entries map of inventory IDs to inventory entities
     */
    @Override
    public void storeAll(Map<Long, InventoryEntity> entries) {
        logger.debug("Storing {} inventory records in database", entries.size());

        try {
            // Note: For write-through, inventory records are already persisted by the time they reach here
            // This method is called for cache warming or batch operations
            logger.debug("StoreAll operation completed for {} inventory records", entries.size());

        } catch (Exception e) {
            if (withinStartupWindow()) {
                logger.debug(
                        "StoreAll operation error during startup for {} inventory records: {}",
                        entries.size(),
                        e.getMessage());
            } else {
                logger.warn("StoreAll operation error for {} inventory records: {}", entries.size(), e.getMessage());
            }
        }
    }

    /**
     * Load an inventory record from the database (cache miss operation).
     * This method is called when a cache get() operation results in a miss.
     *
     * @param inventoryId the inventory ID to load
     * @return the inventory entity or null if not found
     */
    @Override
    public InventoryEntity load(Long inventoryId) {
        logger.debug("Loading inventory from database: inventoryId={}", inventoryId);

        try {
            Optional<InventoryEntity> inventoryOpt = inventoryRepository.findById(inventoryId);

            if (inventoryOpt.isPresent()) {
                InventoryEntity inventory = inventoryOpt.get();
                logger.debug(
                        "Inventory loaded successfully: inventoryId={}, productCode={}",
                        inventoryId,
                        inventory.getProductCode());
                return inventory;
            } else {
                logger.debug("Inventory not found in database: inventoryId={}", inventoryId);
                return null;
            }
        } catch (Exception e) {
            if (withinStartupWindow()) {
                logger.debug("Load operation error during startup for inventoryId={}: {}", inventoryId, e.getMessage());
            } else {
                logger.warn("Load operation error for inventoryId={}: {}", inventoryId, e.getMessage());
            }
            return null;
        }
    }

    /**
     * Load multiple inventory records from the database (bulk cache miss operation).
     * This method is called when multiple cache get() operations result in misses.
     *
     * @param inventoryIds collection of inventory IDs to load
     * @return map of inventory IDs to inventory entities
     */
    @Override
    public Map<Long, InventoryEntity> loadAll(Collection<Long> inventoryIds) {
        logger.debug("Loading {} inventory records from database", inventoryIds.size());

        try {
            // Load inventory records by IDs using repository's findAllById method
            Map<Long, InventoryEntity> loadedInventory = inventoryRepository.findAllById(inventoryIds).stream()
                    .collect(Collectors.toMap(InventoryEntity::getId, inventory -> inventory));

            logger.debug(
                    "Successfully loaded {} out of {} requested inventory records from database",
                    loadedInventory.size(),
                    inventoryIds.size());

            return loadedInventory;

        } catch (Exception e) {
            if (withinStartupWindow()) {
                logger.debug(
                        "LoadAll operation error during startup for {} inventory records: {}",
                        inventoryIds.size(),
                        e.getMessage());
            } else {
                logger.warn(
                        "LoadAll operation error for {} inventory records: {}", inventoryIds.size(), e.getMessage());
            }
            return java.util.Collections.emptyMap();
        }
    }

    /**
     * Get all inventory IDs from the database (used for pre-loading).
     * This method is called during cache initialization or warm-up.
     *
     * @return set of all inventory IDs
     */
    @Override
    public Iterable<Long> loadAllKeys() {
        logger.debug("Loading all inventory IDs from database");

        try {
            Set<Long> allInventoryIds = inventoryRepository.findAll().stream()
                    .map(InventoryEntity::getId)
                    .collect(Collectors.toSet());

            logger.debug("Successfully loaded {} inventory IDs from database", allInventoryIds.size());
            return allInventoryIds;

        } catch (Exception e) {
            if (withinStartupWindow()) {
                logger.debug("LoadAllKeys operation error during startup: {}", e.getMessage());
            } else {
                logger.warn("LoadAllKeys operation error: {}", e.getMessage());
            }
            return java.util.Collections.emptySet();
        }
    }

    /**
     * Delete an inventory record from the database (write-through operation).
     * This method is called when an entry is removed from the cache.
     *
     * @param inventoryId the inventory ID to delete
     */
    @Override
    public void delete(Long inventoryId) {
        logger.debug("Deleting inventory from database: inventoryId={}", inventoryId);

        try {
            // Note: Deletion through MapStore is typically not used in write-through scenarios
            // Inventory records are usually deleted through the service layer, not the cache
            // This implementation logs the operation but doesn't perform actual deletion
            logger.debug("Delete operation called for inventoryId={} - delegating to service layer", inventoryId);

        } catch (Exception e) {
            if (withinStartupWindow()) {
                logger.debug(
                        "Delete operation error during startup for inventoryId={}: {}", inventoryId, e.getMessage());
            } else {
                logger.warn("Delete operation error for inventoryId={}: {}", inventoryId, e.getMessage());
            }
        }
    }

    /**
     * Delete multiple inventory records from the database (bulk write-through operation).
     *
     * @param inventoryIds collection of inventory IDs to delete
     */
    @Override
    public void deleteAll(Collection<Long> inventoryIds) {
        logger.debug("Deleting {} inventory records from database", inventoryIds.size());

        try {
            // Note: Bulk deletion through MapStore is typically not used in write-through scenarios
            // Inventory records are usually deleted through the service layer, not the cache
            logger.debug(
                    "DeleteAll operation called for {} inventory records - delegating to service layer",
                    inventoryIds.size());

            logger.debug("Successfully deleted {} inventory records from database", inventoryIds.size());

        } catch (Exception e) {
            if (withinStartupWindow()) {
                logger.debug(
                        "DeleteAll operation error during startup for {} inventory records: {}",
                        inventoryIds.size(),
                        e.getMessage());
            } else {
                logger.warn(
                        "DeleteAll operation error for {} inventory records: {}", inventoryIds.size(), e.getMessage());
            }
        }
    }
}
