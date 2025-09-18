# Tasks Document

## Configuration Layer Enhancements

- [x] 1. Add inventory TTL configuration property to CacheProperties.java
  - File: src/main/java/com/sivalabs/bookstore/config/CacheProperties.java
  - Add `private int inventoryTimeToLiveSeconds = 1800;` with getter/setter
  - Add validation annotation @Min(0) for the new property
  - Purpose: Externalize inventory TTL instead of hardcoding 1800s in HazelcastConfig
  - _Leverage: Existing @ConfigurationProperties pattern, validation annotations_
  - _Requirements: 1.3_
  - _Prompt: Role: Spring Boot Configuration Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Add inventoryTimeToLiveSeconds property to CacheProperties following requirement 1.3, using existing configuration patterns and validation | Restrictions: Must maintain existing property patterns, use appropriate validation, ensure backward compatibility | _Leverage: Existing @ConfigurationProperties patterns, @Min validation | _Requirements: 1.3 | Success: Property is properly configured with validation, can be externalized via application.properties, maintains existing configuration structure_

- [x] 2. Configure lazy MapStore initialization in HazelcastConfig.java
  - File: src/main/java/com/sivalabs/bookstore/config/HazelcastConfig.java
  - Add `inventoryMapStoreConfig.setInitialLoadMode(MapStoreConfig.InitialLoadMode.LAZY)` to inventory cache configuration
  - Apply same lazy initialization to orders and products MapStore when enabled
  - Purpose: Prevent early `loadAllKeys()` operations during application startup
  - _Leverage: Existing MapStoreConfig patterns, SpringManagedContext setup_
  - _Requirements: 1.1_
  - _Prompt: Role: Hazelcast Configuration Expert | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Configure lazy MapStore initialization following requirement 1.1, applying LAZY mode to prevent early database loading | Restrictions: Must maintain existing SpringManagedContext setup, preserve write-through behavior, ensure MapStore functionality remains intact | _Leverage: Existing MapStoreConfig patterns in HazelcastConfig | _Requirements: 1.1 | Success: MapStore uses lazy initialization, no early loadAllKeys() calls, application startup time improves_

- [x] 3. Use externalized inventory TTL in HazelcastConfig.java
  - File: src/main/java/com/sivalabs/bookstore/config/HazelcastConfig.java
  - Replace hardcoded `inventoryCacheMapConfig.setTimeToLiveSeconds(1800)` with `cacheProperties.getInventoryTimeToLiveSeconds()`
  - Update method signature to include CacheProperties dependency if needed
  - Purpose: Use externalized configuration for inventory cache TTL
  - _Leverage: Existing CacheProperties injection, configuration property binding_
  - _Requirements: 1.3_
  - _Prompt: Role: Spring Boot Configuration Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Replace hardcoded inventory TTL with externalized property following requirement 1.3 | Restrictions: Must maintain existing bean configuration, ensure proper dependency injection, preserve cache behavior | _Leverage: Existing CacheProperties dependency injection | _Requirements: 1.3 | Success: Inventory TTL is configurable via application.properties, no hardcoded values remain, cache behavior is preserved_

## MapStore Pattern Unification

- [x] 4. Enable @SpringAware annotation on ProductMapStore.java
  - File: src/main/java/com/sivalabs/bookstore/catalog/cache/ProductMapStore.java
  - Add `@SpringAware` annotation to class
  - Remove `@Component` annotation to avoid Spring component scanning conflicts
  - Update constructor injection to work with SpringManagedContext
  - Purpose: Unify MapStore wiring pattern across all modules
  - _Leverage: InventoryMapStore pattern, SpringManagedContext from HazelcastConfig_
  - _Requirements: 1.2_
  - _Prompt: Role: Spring Framework Expert | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Enable @SpringAware pattern on ProductMapStore following requirement 1.2, unifying MapStore wiring across modules | Restrictions: Must remove @Component to avoid conflicts, maintain dependency injection, ensure MapStore functionality works with SpringManagedContext | _Leverage: InventoryMapStore @SpringAware pattern, existing SpringManagedContext | _Requirements: 1.2 | Success: ProductMapStore uses consistent @SpringAware pattern, no component scanning conflicts, dependency injection works correctly_

- [x] 5. Enable @SpringAware annotation on OrderMapStore.java
  - File: src/main/java/com/sivalabs/bookstore/orders/cache/OrderMapStore.java
  - Add `@SpringAware` annotation to class
  - Remove `@Component` annotation to avoid Spring component scanning conflicts
  - Update constructor injection to work with SpringManagedContext
  - Purpose: Unify MapStore wiring pattern across all modules
  - _Leverage: InventoryMapStore pattern, SpringManagedContext from HazelcastConfig_
  - _Requirements: 1.2_
  - _Prompt: Role: Spring Framework Expert | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Enable @SpringAware pattern on OrderMapStore following requirement 1.2, maintaining consistency with other MapStores | Restrictions: Must remove @Component to avoid conflicts, preserve existing functionality, maintain transaction behavior | _Leverage: InventoryMapStore @SpringAware pattern, existing SpringManagedContext | _Requirements: 1.2 | Success: OrderMapStore uses consistent @SpringAware pattern, integration works seamlessly, no dependency injection issues_

- [x] 6. Enable MapStore configuration for ProductMapStore in HazelcastConfig.java
  - File: src/main/java/com/sivalabs/bookstore/config/HazelcastConfig.java
  - Uncomment and configure ProductMapStore configuration lines (lines 102-108)
  - Set lazy initialization mode for products MapStore
  - Add SpringManagedContext integration for products cache
  - Purpose: Activate ProductMapStore with consistent configuration
  - _Leverage: InventoryMapStore configuration pattern, existing MapStoreConfig setup_
  - _Requirements: 1.2_
  - _Prompt: Role: Hazelcast Configuration Expert | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Enable ProductMapStore configuration following requirement 1.2, applying consistent patterns with inventory cache | Restrictions: Must use lazy initialization, maintain SpringManagedContext integration, preserve existing cache behavior | _Leverage: InventoryMapStore configuration as template | _Requirements: 1.2 | Success: ProductMapStore is properly configured and active, uses lazy initialization, integrates with SpringManagedContext_

- [x] 7. Enable MapStore configuration for OrderMapStore in HazelcastConfig.java
  - File: src/main/java/com/sivalabs/bookstore/config/HazelcastConfig.java
  - Uncomment and configure OrderMapStore configuration lines (lines 74-80)
  - Set lazy initialization mode for orders MapStore
  - Add SpringManagedContext integration for orders cache
  - Purpose: Activate OrderMapStore with consistent configuration
  - _Leverage: InventoryMapStore configuration pattern, existing MapStoreConfig setup_
  - _Requirements: 1.2_
  - _Prompt: Role: Hazelcast Configuration Expert | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Enable OrderMapStore configuration following requirement 1.2, ensuring consistency across all cache configurations | Restrictions: Must apply lazy initialization, use SpringManagedContext, maintain write-through behavior | _Leverage: InventoryMapStore configuration pattern | _Requirements: 1.2 | Success: OrderMapStore is active and properly configured, uses consistent patterns, integrates seamlessly with cache infrastructure_

## Health Check Optimization

- [x] 8. Replace cache.size() with local statistics in HealthConfig.java
  - File: src/main/java/com/sivalabs/bookstore/config/HealthConfig.java
  - Update `checkOrdersCacheHealth()` to use `ordersCache.getLocalMapStats().getOwnedEntryCount()` instead of `ordersCache.size()`
  - Update `checkProductsCacheHealth()` to use `productsCache.getLocalMapStats().getOwnedEntryCount()` instead of `productsCache.size()`
  - Update `checkInventoryCacheHealth()` to use `inventoryCache.getLocalMapStats().getOwnedEntryCount()` instead of `inventoryCache.size()`
  - Purpose: Avoid triggering MapStore operations during health checks
  - _Leverage: Existing local statistics API, health check infrastructure_
  - _Requirements: 2.1_
  - _Prompt: Role: Hazelcast Performance Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Replace size() calls with local statistics following requirement 2.1, optimizing health checks to avoid MapStore triggers | Restrictions: Must preserve health check accuracy, maintain existing health reporting format, ensure statistics are available | _Leverage: Existing getLocalMapStats() API, health check patterns | _Requirements: 2.1 | Success: Health checks use local statistics, no MapStore operations triggered, health reporting remains accurate and informative_

- [x] 9. Add configurable basic operations mode to CacheProperties.java
  - File: src/main/java/com/sivalabs/bookstore/config/CacheProperties.java
  - Add `private boolean basicOperationsReadOnly = false;` property with getter/setter
  - Add `private boolean testBasicOperationsEnabled = true;` property with getter/setter
  - Purpose: Allow health checks to be configured as read-only or disabled
  - _Leverage: Existing @ConfigurationProperties pattern_
  - _Requirements: 2.2_
  - _Prompt: Role: Spring Boot Configuration Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Add configurable basic operations properties following requirement 2.2, enabling read-only health check mode | Restrictions: Must follow existing property naming conventions, provide sensible defaults, include proper documentation | _Leverage: Existing CacheProperties structure | _Requirements: 2.2 | Success: Properties are properly configured, defaults are appropriate, enables flexible health check configuration_

- [x] 10. Update testBasicOperations() method in HealthConfig.java
  - File: src/main/java/com/sivalabs/bookstore/config/HealthConfig.java
  - Modify `testBasicOperations()` to check `cacheProperties.isTestBasicOperationsEnabled()` before execution
  - Add read-only mode support using `cacheProperties.isBasicOperationsReadOnly()`
  - In read-only mode, skip put/remove operations and only test get operations
  - Purpose: Make health check operations configurable and non-intrusive
  - _Leverage: Existing testBasicOperations implementation, CacheProperties injection_
  - _Requirements: 2.2_
  - _Prompt: Role: Health Monitoring Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Update basic operations testing following requirement 2.2, adding configurable read-only and disable modes | Restrictions: Must maintain health check reliability, preserve existing functionality when not in read-only mode, handle configuration gracefully | _Leverage: Existing testBasicOperations logic, CacheProperties | _Requirements: 2.2 | Success: Basic operations respect configuration settings, read-only mode works correctly, health checks remain reliable and informative_

## Index Cache Implementation

- [x] 11. Create InventoryByProductCodeIndex cache service
  - File: src/main/java/com/sivalabs/bookstore/inventory/cache/InventoryByProductCodeIndex.java
  - Create new service extending AbstractCacheService<String, Long> pattern
  - Implement findInventoryIdByProductCode(), updateIndex(), removeFromIndex() methods
  - Add bidirectional cache maintenance logic
  - Purpose: Provide O(1) lookup for inventory by product code instead of values() scan
  - _Leverage: AbstractCacheService pattern, CacheErrorHandler integration_
  - _Requirements: 3.1_
  - _Prompt: Role: Cache Architecture Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Create index cache service following requirement 3.1, providing efficient product code to inventory ID mapping | Restrictions: Must extend AbstractCacheService, integrate with CacheErrorHandler, maintain cache consistency, handle error scenarios gracefully | _Leverage: AbstractCacheService base class, existing cache service patterns | _Requirements: 3.1 | Success: Index cache provides O(1) lookups, bidirectional maintenance works correctly, integrates seamlessly with error handling_

- [x] 12. Add inventory-by-product-code cache configuration to HazelcastConfig.java
  - File: src/main/java/com/sivalabs/bookstore/config/HazelcastConfig.java
  - Add new MapConfig for "inventory-by-product-code-cache" with String keys and Long values
  - Configure appropriate TTL, eviction policy, and statistics
  - Create @Bean for IMap<String, Long> inventoryByProductCodeCache
  - Purpose: Provide Hazelcast cache infrastructure for product code index
  - _Leverage: Existing cache configuration patterns, MapConfig setup_
  - _Requirements: 3.1_
  - _Prompt: Role: Hazelcast Configuration Expert | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Configure index cache infrastructure following requirement 3.1, setting up Hazelcast map for product code indexing | Restrictions: Must follow existing cache configuration patterns, use appropriate key/value types (String/Long), configure reasonable TTL and eviction | _Leverage: Existing MapConfig patterns, cache bean configuration | _Requirements: 3.1 | Success: Index cache is properly configured, bean is available for injection, configuration follows existing patterns_

- [x] 13. Integrate index cache with InventoryCacheService.java
  - File: src/main/java/com/sivalabs/bookstore/inventory/cache/InventoryCacheService.java
  - Inject InventoryByProductCodeIndex into constructor
  - Update `findByProductCode()` method to use index cache instead of values() scan
  - Add bidirectional cache maintenance in `cacheInventory()` and `updateCachedInventory()` methods
  - Purpose: Replace inefficient values() scan with O(1) index lookup
  - _Leverage: Existing InventoryCacheService structure, dependency injection pattern_
  - _Requirements: 3.1, 3.2_
  - _Prompt: Role: Cache Integration Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Integrate index cache following requirements 3.1 and 3.2, replacing values() scan with efficient index lookup | Restrictions: Must maintain existing public API, ensure bidirectional consistency, handle cache errors gracefully, preserve fallback behavior | _Leverage: Existing cache service patterns, dependency injection | _Requirements: 3.1, 3.2 | Success: Product code lookup uses index cache, bidirectional maintenance works correctly, performance improves significantly, API remains backward compatible_

## Batch Query Support

- [x] 14. Add batch query method to ProductRepository.java
  - File: src/main/java/com/sivalabs/bookstore/catalog/domain/ProductRepository.java
  - Add method signature: `List<ProductEntity> findByCodeIn(Collection<String> codes);`
  - Use Spring Data JPA query derivation for batch lookup
  - Purpose: Enable batch loading of products by multiple codes
  - _Leverage: Spring Data JPA query derivation, existing repository patterns_
  - _Requirements: 3.3_
  - _Prompt: Role: Spring Data JPA Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Add batch query method following requirement 3.3, enabling efficient multi-product lookups | Restrictions: Must use Spring Data JPA conventions, follow existing repository patterns, ensure query performance is optimized | _Leverage: Spring Data JPA query derivation, existing repository interface patterns | _Requirements: 3.3 | Success: Batch query method is properly defined, uses JPA query derivation, enables efficient multi-entity loading_

- [x] 15. Add batch query method to OrderRepository.java
  - File: src/main/java/com/sivalabs/bookstore/orders/domain/OrderRepository.java
  - Add method signature: `List<OrderEntity> findByOrderNumberIn(Collection<String> orderNumbers);`
  - Use Spring Data JPA query derivation for batch lookup
  - Purpose: Enable batch loading of orders by multiple order numbers
  - _Leverage: Spring Data JPA query derivation, existing repository patterns_
  - _Requirements: 3.3_
  - _Prompt: Role: Spring Data JPA Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Add batch query method following requirement 3.3, providing efficient multi-order lookups | Restrictions: Must follow Spring Data JPA conventions, maintain consistency with existing repository methods, optimize for performance | _Leverage: Spring Data JPA query derivation, repository interface patterns | _Requirements: 3.3 | Success: Batch query method is correctly implemented, follows JPA conventions, enables efficient batch loading_

- [x] 16. Update ProductMapStore.loadAll() to use batch queries
  - File: src/main/java/com/sivalabs/bookstore/catalog/cache/ProductMapStore.java
  - Replace individual `findByCode()` calls in `loadAll()` method with `findByCodeIn()` batch query
  - Maintain error handling for partial results
  - Optimize performance by reducing database round trips
  - Purpose: Improve MapStore bulk loading performance
  - _Leverage: New ProductRepository.findByCodeIn() method, existing error handling patterns_
  - _Requirements: 3.3_
  - _Prompt: Role: MapStore Optimization Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Optimize loadAll() with batch queries following requirement 3.3, reducing database round trips | Restrictions: Must maintain existing error handling, preserve partial result capability, ensure transaction consistency | _Leverage: New findByCodeIn() method, existing loadAll error handling | _Requirements: 3.3 | Success: loadAll() uses batch queries, performance improves significantly, error handling remains robust, partial results are supported_

- [x] 17. Update OrderMapStore.loadAll() to use batch queries
  - File: src/main/java/com/sivalabs/bookstore/orders/cache/OrderMapStore.java
  - Replace individual order loading logic in `loadAll()` method with `findByOrderNumberIn()` batch query
  - Implement proper key-value mapping for order numbers to entities
  - Maintain existing error handling and partial result support
  - Purpose: Improve MapStore bulk loading performance for orders
  - _Leverage: New OrderRepository.findByOrderNumberIn() method, existing MapStore patterns_
  - _Requirements: 3.3_
  - _Prompt: Role: MapStore Optimization Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Optimize order loadAll() following requirement 3.3, implementing efficient batch loading | Restrictions: Must handle order number to entity mapping correctly, maintain error resilience, preserve existing loadAll behavior | _Leverage: New findByOrderNumberIn() method, existing error handling patterns | _Requirements: 3.3 | Success: Order loadAll() uses batch queries, performance is optimized, key mapping works correctly, error handling is preserved_

## Module Boundary Strengthening

- [x] 18. Create package-info.java for catalog module
  - File: src/main/java/com/sivalabs/bookstore/catalog/package-info.java
  - Add `@ApplicationModule` annotation with appropriate allowedDependencies
  - Define `@NamedInterface` for public API components (ProductApi)
  - Purpose: Explicitly define catalog module boundaries and public APIs
  - _Leverage: Spring Modulith annotations, existing module structure_
  - _Requirements: 4.1, 4.2_
  - _Prompt: Role: Spring Modulith Architect | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Define catalog module boundaries following requirements 4.1 and 4.2, using Spring Modulith annotations | Restrictions: Must not break existing module dependencies, ensure public APIs are properly exposed, maintain module isolation | _Leverage: Spring Modulith @ApplicationModule and @NamedInterface | _Requirements: 4.1, 4.2 | Success: Module boundaries are explicitly defined, public APIs are marked, no module boundary violations_

- [x] 19. Create package-info.java for orders module
  - File: src/main/java/com/sivalabs/bookstore/orders/package-info.java
  - Add `@ApplicationModule(allowedDependencies = {"catalog", "common"})` annotation
  - Define `@NamedInterface` for public API components (OrderApi)
  - Purpose: Explicitly define orders module boundaries and dependencies
  - _Leverage: Spring Modulith annotations, existing dependency patterns_
  - _Requirements: 4.1, 4.2, 4.3_
  - _Prompt: Role: Spring Modulith Architect | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Define orders module boundaries following requirements 4.1, 4.2, and 4.3, maintaining dependency patterns | Restrictions: Must preserve existing orders→catalog dependency, include common-cache dependency, ensure API boundaries are correct | _Leverage: Spring Modulith module definition patterns | _Requirements: 4.1, 4.2, 4.3 | Success: Orders module boundaries are correctly defined, dependencies are explicit, public APIs are properly marked_

- [x] 20. Create package-info.java for inventory module
  - File: src/main/java/com/sivalabs/bookstore/inventory/package-info.java
  - Add `@ApplicationModule(allowedDependencies = {"common"})` annotation
  - Define `@NamedInterface` for public API components (InventoryApi)
  - Purpose: Explicitly define inventory module boundaries and minimal dependencies
  - _Leverage: Spring Modulith annotations, module isolation principles_
  - _Requirements: 4.1, 4.2_
  - _Prompt: Role: Spring Modulith Architect | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Define inventory module boundaries following requirements 4.1 and 4.2, ensuring minimal dependencies | Restrictions: Must maintain module isolation, only depend on common module, preserve existing event-driven communication | _Leverage: Spring Modulith module patterns, event-driven architecture | _Requirements: 4.1, 4.2 | Success: Inventory module is properly isolated, dependencies are minimal, public APIs are well-defined_

## Build & CI Alignment

- [x] 21. Update CI Java version in maven.yml workflow
  - File: .github/workflows/maven.yml
  - Update `java-version` from current value to `21` to match pom.xml
  - Ensure consistency between local development and CI environment
  - Purpose: Align CI pipeline with local development Java version
  - _Leverage: Existing GitHub Actions workflow configuration_
  - _Requirements: 5.1_
  - _Prompt: Role: DevOps Engineer | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Align CI Java version following requirement 5.1, ensuring consistency with local development | Restrictions: Must maintain existing workflow structure, preserve all build steps, ensure compatibility with all dependencies | _Leverage: Existing GitHub Actions configuration | _Requirements: 5.1 | Success: CI uses Java 21 consistently with pom.xml, builds run successfully, no compatibility issues_

- [x] 22. Externalize Liquibase connection configuration in pom.xml
  - File: pom.xml
  - Move Liquibase plugin connection properties to Maven profiles or environment variables
  - Add comments indicating runtime configuration should use `spring.liquibase.*` properties
  - Purpose: Remove hardcoded database connection from build configuration
  - _Leverage: Maven profiles, environment variable substitution_
  - _Requirements: 5.2_
  - _Prompt: Role: Maven Build Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Externalize Liquibase configuration following requirement 5.2, removing hardcoded connection details | Restrictions: Must preserve Liquibase functionality, maintain build compatibility, ensure runtime configuration works properly | _Leverage: Maven profiles and environment variable patterns | _Requirements: 5.2 | Success: Liquibase configuration is externalized, no hardcoded credentials, build and runtime work correctly_

## Error Handling & Logging Improvements

- [x] 23. Update MapStore logging levels for startup scenarios
  - Files:
    - src/main/java/com/sivalabs/bookstore/inventory/cache/InventoryMapStore.java
    - src/main/java/com/sivalabs/bookstore/catalog/cache/ProductMapStore.java
    - src/main/java/com/sivalabs/bookstore/orders/cache/OrderMapStore.java
  - Change repository unavailability logging during startup from WARN to DEBUG level
  - Ensure unexpected exceptions still log at WARN/ERROR for proper alerting
  - Add startup state detection to adjust logging behavior
  - Purpose: Reduce noise during application startup while maintaining error visibility
  - _Leverage: Existing logging infrastructure, SLF4J patterns_
  - _Requirements: 6.1, 6.3_
  - _Prompt: Role: Logging and Monitoring Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Optimize MapStore logging following requirements 6.1 and 6.3, reducing startup noise while preserving error alerting | Restrictions: Must preserve critical error logging, maintain debugging capabilities, avoid over-suppressing important warnings | _Leverage: Existing SLF4J logging patterns | _Requirements: 6.1, 6.3 | Success: Startup logging is appropriate, noise is reduced, critical errors remain visible, debugging information is available when needed_

- [x] 24. Add configurable CacheErrorHandler thresholds to CacheProperties.java
  - File: src/main/java/com/sivalabs/bookstore/config/CacheProperties.java
  - Add `private int circuitBreakerFailureThreshold = 5;` property with getter/setter
  - Add `private long circuitBreakerRecoveryTimeoutMs = 30000;` property with getter/setter
  - Purpose: Make CacheErrorHandler thresholds configurable by environment
  - _Leverage: Existing @ConfigurationProperties pattern_
  - _Requirements: 6.2_
  - _Prompt: Role: Circuit Breaker Configuration Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Add configurable circuit breaker thresholds following requirement 6.2, enabling environment-specific tuning | Restrictions: Must provide sensible defaults, follow existing property naming conventions, include proper validation | _Leverage: Existing CacheProperties structure | _Requirements: 6.2 | Success: Circuit breaker thresholds are configurable, defaults are appropriate, validation is in place_

- [x] 25. Update CacheErrorHandler to use configurable thresholds
  - File: src/main/java/com/sivalabs/bookstore/common/cache/CacheErrorHandler.java
  - Inject CacheProperties and use configurable failure threshold and recovery timeout
  - Replace hardcoded constants with property-based configuration
  - Maintain backward compatibility with existing behavior
  - Purpose: Enable environment-specific circuit breaker tuning
  - _Leverage: Existing CacheErrorHandler structure, dependency injection patterns_
  - _Requirements: 6.2_
  - _Prompt: Role: Circuit Breaker Implementation Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Update CacheErrorHandler following requirement 6.2, using configurable thresholds for environment-specific behavior | Restrictions: Must maintain existing circuit breaker behavior as default, preserve API compatibility, ensure proper dependency injection | _Leverage: Existing circuit breaker logic, CacheProperties injection | _Requirements: 6.2 | Success: CacheErrorHandler uses configurable thresholds, behavior is environment-tunable, backward compatibility is maintained_

## Testing and Validation

- [x] 26. Create tests for inventory TTL configuration
  - File: src/test/java/com/sivalabs/bookstore/config/CachePropertiesTests.java
  - Test externalized inventory TTL property loading and validation
  - Test configuration binding from application properties
  - Purpose: Ensure configuration changes work correctly
  - _Leverage: Existing Spring Boot test patterns, @ConfigurationPropertiesTest_
  - _Requirements: 7.1, 7.2_
  - _Prompt: Role: Configuration Testing Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Create configuration tests following requirements 7.1 and 7.2, validating property binding and TTL configuration | Restrictions: Must test both valid and invalid configurations, ensure validation works properly, follow existing test patterns | _Leverage: Spring Boot configuration testing patterns | _Requirements: 7.1, 7.2 | Success: Configuration tests cover all new properties, validation is tested, property binding works correctly_

- [x] 27. Create tests for lazy MapStore initialization
  - File: src/test/java/com/sivalabs/bookstore/config/HazelcastConfigTests.java
  - Test that MapStore uses LAZY initialization mode
  - Verify no early `loadAllKeys()` calls during configuration
  - Test SpringManagedContext integration with @SpringAware MapStores
  - Purpose: Validate lazy initialization and wiring pattern changes
  - _Leverage: Existing HazelcastConfigTests, TestContainers integration_
  - _Requirements: 7.1, 7.3_
  - _Prompt: Role: Cache Integration Testing Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Create lazy initialization tests following requirements 7.1 and 7.3, validating MapStore behavior | Restrictions: Must not trigger actual database operations during tests, ensure lazy behavior is properly tested, validate SpringManagedContext integration | _Leverage: Existing cache testing patterns, test infrastructure | _Requirements: 7.1, 7.3 | Success: Lazy initialization is properly tested, no early database calls, SpringManagedContext integration works_

- [x] 28. Create tests for index cache functionality
  - File: src/test/java/com/sivalabs/bookstore/inventory/cache/InventoryByProductCodeIndexTests.java
  - Test O(1) lookup performance vs values() scan
  - Test bidirectional cache maintenance consistency
  - Test error handling and circuit breaker integration
  - Purpose: Validate index cache performance and reliability
  - _Leverage: Existing AbstractCacheServiceTests pattern, performance testing utilities_
  - _Requirements: 7.2, 7.4_
  - _Prompt: Role: Cache Performance Testing Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Create index cache tests following requirements 7.2 and 7.4, validating performance and consistency | Restrictions: Must test both performance characteristics and correctness, ensure bidirectional consistency, validate error handling scenarios | _Leverage: Existing cache service test patterns | _Requirements: 7.2, 7.4 | Success: Index cache performance is validated, consistency is tested, error scenarios are covered_

- [x] 29. Create tests for batch query optimization
  - File: src/test/java/com/sivalabs/bookstore/catalog/cache/ProductMapStoreTests.java and src/test/java/com/sivalabs/bookstore/orders/cache/OrderMapStoreTests.java
  - Test `loadAll()` methods use batch queries instead of individual calls
  - Verify performance improvement with large datasets
  - Test error handling for partial batch results
  - Purpose: Validate batch query optimization and error resilience
  - _Leverage: Existing MapStore test patterns, TestContainers for integration testing_
  - _Requirements: 7.2, 7.3_
  - _Prompt: Role: Database Performance Testing Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Create batch query tests following requirements 7.2 and 7.3, validating performance optimization | Restrictions: Must test actual performance improvements, validate partial result handling, ensure database interactions are correct | _Leverage: Existing MapStore test infrastructure, TestContainers | _Requirements: 7.2, 7.3 | Success: Batch queries are properly tested, performance improvements are validated, error handling works correctly_

- [x] 30. Create tests for health check optimization
  - File: src/test/java/com/sivalabs/bookstore/config/HealthConfigTests.java
  - Test that health checks use local statistics instead of size()
  - Test configurable basic operations modes (read-only, disabled)
  - Verify no MapStore operations are triggered during health checks
  - Purpose: Validate health check performance optimization
  - _Leverage: Existing health check test patterns, mock verification_
  - _Requirements: 7.2, 7.5_
  - _Prompt: Role: Health Monitoring Testing Specialist | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Create health check optimization tests following requirements 7.2 and 7.5, ensuring no performance impact | Restrictions: Must verify no MapStore triggers, test all configuration modes, ensure health accuracy is maintained | _Leverage: Existing health check testing infrastructure | _Requirements: 7.2, 7.5 | Success: Health checks don't trigger MapStore operations, configuration modes work correctly, health accuracy is preserved_

- [x] 31. Run full test suite validation
  - Files: All existing test files
  - Execute `./mvnw clean verify` to ensure all existing tests pass
  - Run module-specific integration tests with `@ApplicationModuleTest`
  - Verify ModularityTests pass with new module boundaries
  - Purpose: Ensure all changes maintain system integrity and test coverage
  - _Leverage: Existing test infrastructure, Maven test execution_
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  - _Prompt: Role: Quality Assurance Engineer | Task: Implement the task for spec cache-optimization, first run spec-workflow-guide to get the workflow guide then implement the task: Execute comprehensive test validation covering all requirements 7.1-7.5, ensuring system integrity | Restrictions: All tests must pass without failures, no regression in performance-sensitive tests, module boundaries must be validated | _Leverage: Existing test execution infrastructure, Maven verify lifecycle | _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5 | Success: All tests pass without failures, no performance regressions, module boundaries are validated, system integrity is maintained_
