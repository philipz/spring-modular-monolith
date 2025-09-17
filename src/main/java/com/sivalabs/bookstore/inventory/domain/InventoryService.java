package com.sivalabs.bookstore.inventory.domain;

import com.sivalabs.bookstore.inventory.cache.InventoryCacheService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final InventoryRepository inventoryRepository;
    private final InventoryCacheService inventoryCacheService;

    InventoryService(
            InventoryRepository inventoryRepository,
            @Autowired(required = false) InventoryCacheService inventoryCacheService) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryCacheService = inventoryCacheService;

        if (inventoryCacheService != null) {
            log.info("InventoryService initialized with cache support enabled");
        } else {
            log.info("InventoryService initialized without cache support (cache disabled or unavailable)");
        }
    }

    /**
     * Check if cache service is available and operational.
     *
     * @return true if cache service is available, false otherwise
     */
    private boolean isCacheAvailable() {
        return inventoryCacheService != null && !inventoryCacheService.isCircuitBreakerOpen();
    }

    @Transactional
    public void decreaseStockLevel(String productCode, int quantity) {
        log.info("Decrease stock level for product code {} and quantity {}", productCode, quantity);

        // Find inventory record - use cache if available with database fallback
        Optional<InventoryEntity> inventoryOpt;
        if (isCacheAvailable()) {
            try {
                inventoryOpt = inventoryCacheService.findByProductCodeWithFallback(
                        productCode, () -> inventoryRepository.findByProductCode(productCode));
            } catch (Exception e) {
                log.warn(
                        "Cache lookup failed for product code {} - using database directly: {}",
                        productCode,
                        e.getMessage());
                inventoryOpt = inventoryRepository.findByProductCode(productCode);
            }
        } else {
            log.debug("Cache service unavailable - querying database directly for product code: {}", productCode);
            inventoryOpt = inventoryRepository.findByProductCode(productCode);
        }

        if (inventoryOpt.isPresent()) {
            InventoryEntity inventory = inventoryOpt.get();
            long newQuantity = inventory.getQuantity() - quantity;
            inventory.setQuantity(newQuantity);

            // Save to database first
            InventoryEntity savedInventory = inventoryRepository.save(inventory);
            log.info("Updated stock level for product code {} to: {}", productCode, newQuantity);

            // Update cache after successful database save
            if (isCacheAvailable()) {
                try {
                    inventoryCacheService.updateCachedInventory(savedInventory.getId(), savedInventory);
                    log.debug("Inventory updated in cache: product code {}", productCode);
                } catch (Exception e) {
                    log.warn(
                            "Failed to update inventory in cache for product code {} - database update successful: {}",
                            productCode,
                            e.getMessage());
                    // Continue - cache failure should not break business logic
                }
            }
        } else {
            log.warn("Invalid product code {}", productCode);
        }
    }

    @Transactional(readOnly = true)
    public Long getStockLevel(String productCode) {
        log.debug("Getting stock level for product code: {}", productCode);

        // Use unified cache fallback logic
        Optional<InventoryEntity> inventoryOpt;
        if (isCacheAvailable()) {
            try {
                inventoryOpt = inventoryCacheService.findByProductCodeWithFallback(
                        productCode, () -> inventoryRepository.findByProductCode(productCode));
            } catch (Exception e) {
                log.warn(
                        "Cache lookup failed for product code {} - using database directly: {}",
                        productCode,
                        e.getMessage());
                inventoryOpt = inventoryRepository.findByProductCode(productCode);
            }
        } else {
            log.debug("Cache service unavailable - querying database directly for product code: {}", productCode);
            inventoryOpt = inventoryRepository.findByProductCode(productCode);
        }

        if (isCacheAvailable() && inventoryOpt.isPresent()) {
            InventoryEntity inventory = inventoryOpt.get();
            try {
                boolean cached = inventoryCacheService.cacheInventory(inventory.getId(), inventory);
                if (cached) {
                    log.debug(
                            "Inventory cached after lookup for product code {} with inventory id {}",
                            productCode,
                            inventory.getId());
                } else {
                    log.debug(
                            "Inventory cache operation skipped or returned false for product code {} with inventory id {}",
                            productCode,
                            inventory.getId());
                }
            } catch (Exception cacheException) {
                log.warn(
                        "Failed to cache inventory for product code {} after lookup: {}",
                        productCode,
                        cacheException.getMessage());
                log.debug(
                        "Cache exception details while caching inventory for product code {}",
                        productCode,
                        cacheException);
            }
        }

        Long stock = inventoryOpt.map(InventoryEntity::getQuantity).orElse(0L);
        log.info("Stock level for product code {} is: {}", productCode, stock);
        return stock;
    }
}
