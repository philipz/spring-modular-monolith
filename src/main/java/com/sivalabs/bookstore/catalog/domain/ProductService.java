package com.sivalabs.bookstore.catalog.domain;

import com.sivalabs.bookstore.catalog.cache.ProductCacheService;
import com.sivalabs.bookstore.catalog.support.PagedResults;
import com.sivalabs.bookstore.common.models.PagedResult;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final int PRODUCT_PAGE_SIZE = 10;
    private final ProductRepository repo;
    private final ProductCacheService productCacheService;

    ProductService(ProductRepository repo, @Autowired(required = false) ProductCacheService productCacheService) {
        this.repo = repo;
        this.productCacheService = productCacheService;

        if (productCacheService != null) {
            log.info("ProductService initialized with cache support enabled");
        } else {
            log.info("ProductService initialized without cache support (cache disabled or unavailable)");
        }
    }

    /**
     * Check if cache service is available and operational.
     *
     * @return true if cache service is available, false otherwise
     */
    private boolean isCacheAvailable() {
        return productCacheService != null && !productCacheService.isCircuitBreakerOpen();
    }

    @Transactional(readOnly = true)
    public PagedResult<ProductEntity> getProducts(int pageNo) {
        Sort sort = Sort.by("name").ascending();
        int page = pageNo <= 1 ? 0 : pageNo - 1;
        Pageable pageable = PageRequest.of(page, PRODUCT_PAGE_SIZE, sort);
        Page<ProductEntity> productsPage = repo.findAll(pageable);
        return PagedResults.fromPage(productsPage);
    }

    @Transactional(readOnly = true)
    public Optional<ProductEntity> getByCode(String code) {
        // Try cache first if available
        if (isCacheAvailable()) {
            try {
                Optional<ProductEntity> cachedProduct = productCacheService.findByProductCode(code);
                if (cachedProduct.isPresent()) {
                    log.debug("Product found in cache: {}", code);
                    return cachedProduct;
                }
                log.debug("Cache miss for product code: {}", code);
            } catch (Exception e) {
                log.warn(
                        "Failed to read from cache for product code {} - falling back to database: {}",
                        code,
                        e.getMessage());
            }
        } else {
            log.debug("Cache service unavailable - querying database directly for product code: {}", code);
        }

        // Cache miss or cache unavailable - query database
        Optional<ProductEntity> product = repo.findByCode(code);

        // Cache the result if found and cache is available
        if (product.isPresent() && isCacheAvailable()) {
            try {
                productCacheService.cacheProduct(code, product.get());
                log.debug("Product cached after database retrieval: product code {}", code);
            } catch (Exception e) {
                log.warn("Failed to cache product {} after database retrieval: {}", code, e.getMessage());
                // Continue with returning the product - cache failure should not affect read operation
            }
        }

        return product;
    }
}
