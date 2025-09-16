# Tasks Document

## Phase 1: Architecture Verification and Documentation

- [x] 1. Verify and document modules in monolith
  - File: src/test/java/com/sivalabs/bookstore/ModularityTests.java
  - Ensure ApplicationModules.verify() runs and add PlantUML documentation generation via Documenter
  - Generate module diagrams into target/modulith-docs for baseline architecture
  - Purpose: Continuously validate boundaries and produce architecture docs before extraction
  - _Leverage: src/test/java/com/sivalabs/bookstore/ModularityTests.java_
  - _Requirements: 1.1_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Architecture Test Engineer with Spring Modulith expertise | Task: Update ModularityTests to verify modules and write PlantUML docs, ensuring reproducible architecture documentation | Restrictions: Do not remove existing tests; only add Documenter calls and keep tests green | _Leverage: src/test/java/com/sivalabs/bookstore/ModularityTests.java_ | _Requirements: 1.1_ | Success: modules.verify() passes and PlantUML docs are generated under target/modulith-docs | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [x] 2. Add architecture test template to new orders service
  - File: orders/src/test/java/com/sivalabs/bookstore/ModularityTests.java
  - Create ApplicationModules.of(OrdersApplication.class).verify() test and Documenter docs generation
  - Align package with new service main class (e.g., com.sivalabs.bookstore.orders.OrdersApplication)
  - Purpose: Keep module verification in the extracted service from day one
  - _Leverage: src/test/java/com/sivalabs/bookstore/ModularityTests.java_
  - _Requirements: 1.2_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring Boot Test Engineer with Modulith verification experience | Task: Create architecture verification test for the new orders service mirroring monolith setup | Restrictions: Use the new service main application class, keep test minimal and green | _Leverage: src/test/java/com/sivalabs/bookstore/ModularityTests.java_ | _Requirements: 1.2_ | Success: New test compiles in the orders service and generates documentation into target/modulith-docs | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

## Phase 2: Pre-Migration Analysis and Preparation

- [x] 3. Analyze and validate module boundaries and coupling
  - File: analysis documentation and module boundary validation
  - Analyze current module dependencies, public APIs, and coupling levels
  - Validate that Orders module has low coupling and high cohesion suitable for extraction
  - Purpose: Ensure the Orders module is a good candidate for extraction with minimal risk
  - _Leverage: Spring Modulith verification tests, dependency analysis tools_
  - _Requirements: 1.1_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Software Architect specializing in module boundary analysis and coupling assessment | Task: Perform comprehensive analysis of Orders module boundaries, dependencies, and coupling levels following requirement 1.1, validating extraction feasibility and risk assessment | Restrictions: Must analyze actual code dependencies, measure coupling metrics, identify all integration points without making changes | _Leverage: Spring Modulith verification tests, dependency analysis tools_ | _Requirements: 1.1_ | Success: Module boundary analysis complete with coupling metrics, risk assessment documented, extraction feasibility confirmed with low-risk profile | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [x] 4. Refactor and strengthen module API boundaries
  - File: src/main/java/com/sivalabs/bookstore/orders/OrdersApi.java, package-info.java
  - Review and strengthen the public API contract for the Orders module
  - Ensure clean separation between public API and internal implementation
  - Purpose: Create well-defined, stable API contracts for external integration
  - _Leverage: existing OrdersApi and API design patterns_
  - _Requirements: 1.4, 1.5_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: API Design Architect with module boundary expertise | Task: Review and strengthen Orders module API boundaries following requirements 1.4, 1.5, ensuring clean separation and stable contracts for extraction | Restrictions: Must preserve existing API contracts, strengthen boundaries without breaking compatibility, improve encapsulation | _Leverage: existing OrdersApi and API design patterns_ | _Requirements: 1.4, 1.5_ | Success: API boundaries strengthened with clear contracts, public/private separation enforced, API documentation complete and stable | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [x] 5. Implement data encapsulation and ownership validation
  - File: database analysis, schema validation, data access review
  - Validate complete data ownership within Orders module and identify shared data concerns
  - Ensure Orders module has exclusive ownership of its data entities and relationships
  - Purpose: Confirm data isolation and identify potential data migration challenges
  - _Leverage: existing database schema, JPA entity mappings_
  - _Requirements: 3.1, 3.5_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Data Architect with database design and data ownership expertise | Task: Validate data encapsulation and ownership for Orders module following requirements 3.1, 3.5, identifying shared data concerns and migration challenges | Restrictions: Must analyze actual data relationships, identify foreign key constraints, map data dependencies without making schema changes | _Leverage: existing database schema, JPA entity mappings_ | _Requirements: 3.1, 3.5_ | Success: Data ownership validated, no shared data concerns identified, migration strategy planned for independent database | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

### Phase 3: Additions

- [x] 6. Enforce port usage and NamedInterface compliance across modules
  - File: src/main/java/com/sivalabs/bookstore/orders/api/package-info.java, src/main/java/com/sivalabs/bookstore/orders/OrdersApi.java
  - Audit cross-module imports to ensure only orders.api is consumed externally; expand OrdersApi if new ports are needed
  - Strengthen @NamedInterface exposure to include API packages actually consumed externally
  - Purpose: Prevent other modules from importing orders internals and ensure stable contracts
  - _Leverage: src/test/java/com/sivalabs/bookstore/ModularityTests.java_
  - _Requirements: 1.4, 1.5_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: API Governance Engineer with Modulith boundary enforcement expertise | Task: Enforce that only orders.api is imported by other modules; update OrdersApi and @NamedInterface as needed | Restrictions: Preserve backward compatible API contracts; do not expose internal implementations | _Leverage: src/test/java/com/sivalabs/bookstore/ModularityTests.java_ | _Requirements: 1.4, 1.5_ | Success: No cross-module imports of orders internals; ApplicationModules.verify() passes | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [x] 7. Move OrderCreatedEvent to Orders API and update consumers
  - File: src/main/java/com/sivalabs/bookstore/orders/api/events/OrderCreatedEvent.java, src/main/java/com/sivalabs/bookstore/orders/domain/models/OrderCreatedEvent.java (remove/migrate)
  - Relocate OrderCreatedEvent to orders.api.events, keep @Externalized routing key intact, update imports in publishers/consumers
  - Add/adjust @NamedInterface to expose events package as part of public API
  - Purpose: Make domain events part of the public API surface to avoid internal leaks
  - _Leverage: src/main/java/com/sivalabs/bookstore/orders/domain/models/OrderCreatedEvent.java, src/main/java/com/sivalabs/bookstore/notifications/OrderEventNotificationHandler.java, src/main/java/com/sivalabs/bookstore/inventory/OrderEventInventoryHandler.java_
  - _Requirements: 1.4, 2.2_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Domain Event Architect with Modulith event design expertise | Task: Move OrderCreatedEvent into orders.api.events and update all references while preserving externalized routing | Restrictions: Do not change event payload shape or routing; ensure consumers remain functional | _Leverage: existing event class and handlers_ | _Requirements: 1.4, 2.2_ | Success: Event class available under orders.api.events, handlers compile and tests pass | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [x] 8. Decouple orders-specific Hazelcast MapStore config from global config
  - File: src/main/java/com/sivalabs/bookstore/config/HazelcastConfig.java, src/main/java/com/sivalabs/bookstore/orders/config/HazelcastOrderCacheConfig.java
  - Move orders MapStore and map configuration into the orders module to avoid config → orders internal dependency
  - Keep cluster/base config in config module; expose only necessary types via orders API if needed
  - Purpose: Remove cross-module internal reference and keep cache wiring within owning module
  - _Leverage: src/main/java/com/sivalabs/bookstore/orders/cache/OrderMapStore.java, src/main/java/com/sivalabs/bookstore/config/HazelcastConfig.java_
  - _Requirements: 1.4, 6.1_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Infrastructure Architect with caching expertise | Task: Relocate orders-specific Hazelcast MapStore configuration into the orders module and remove direct references from global config | Restrictions: Preserve existing map names and behavior; avoid breaking cache initialization | _Leverage: existing MapStore and config_ | _Requirements: 1.4, 6.1_ | Success: Global config no longer imports orders internals; cache initializes correctly via orders-local config | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

## Phase 4: Infrastructure Preparation and New Service Setup

- [x] 9. Create new microservice project structure with proper Maven setup
  - File: orders/pom.xml, orders/src/main/java/com/sivalabs/bookstore/orders/, orders/src/main/resources/
  - Create complete Spring Boot project structure in /orders directory
  - Configure Maven with appropriate dependencies and independent build system
  - Purpose: Establish foundation infrastructure for the extracted microservice
  - _Leverage: existing pom.xml structure and Spring Boot configuration patterns_
  - _Requirements: 1.1, 1.2, 1.3_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring Boot Project Architect with Maven build system expertise | Task: Create new microservice project structure following requirements 1.1, 1.2, 1.3, establishing independent Spring Boot application with proper Maven configuration | Restrictions: Must use compatible versions with existing stack, create independent build system, do not interfere with existing project structure | _Leverage: existing pom.xml structure and Spring Boot configuration patterns_ | _Requirements: 1.1, 1.2, 1.3_ | Success: Complete project structure created, Maven build functional, dependencies properly configured, independent build system operational | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [x] 10. Set up independent database and migration infrastructure
  - File: orders/src/main/resources/db/migration/, database configuration
  - Create dedicated PostgreSQL 17 database setup with Liquibase migrations
  - Establish complete schema creation and data migration scripts
  - Purpose: Provide independent data persistence layer for the microservice
  - _Leverage: existing Liquibase migration scripts V4 and V5_
  - _Requirements: 3.1, 3.2, 3.3, 3.4_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Database Administrator with PostgreSQL and Liquibase migration expertise | Task: Set up independent database infrastructure following requirements 3.1, 3.2, 3.3, 3.4, creating dedicated PostgreSQL 17 database with complete migration scripts | Restrictions: Must maintain exact schema structure, preserve data integrity, use independent database instance without cross-references | _Leverage: existing Liquibase migration scripts V4 and V5_ | _Requirements: 3.1, 3.2, 3.3, 3.4_ | Success: Independent database created, migration scripts functional, schema creates properly, connection configuration complete | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [x] 11. Configure messaging infrastructure for event publishing
  - File: orders/src/main/resources/application.properties, messaging configuration classes
  - Set up RabbitMQ configuration for OrderCreatedEvent publishing
  - Ensure events maintain compatibility with existing downstream consumers
  - Purpose: Maintain event-driven architecture with external services
  - _Leverage: existing RabbitMQ configuration and event publishing patterns_
  - _Requirements: 5.1, 5.2, 5.3, 5.4_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Messaging Infrastructure Specialist with RabbitMQ and Spring AMQP expertise | Task: Configure messaging infrastructure following requirements 5.1, 5.2, 5.3, 5.4, setting up RabbitMQ for event publishing with downstream compatibility | Restrictions: Must maintain exact event schema, preserve routing keys and exchange configuration, ensure backward compatibility | _Leverage: existing RabbitMQ configuration and event publishing patterns_ | _Requirements: 5.1, 5.2, 5.3, 5.4_ | Success: RabbitMQ configuration complete, event publishing functional, downstream compatibility maintained, message routing operational | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

### Phase 5: Additions

- [x] 12. Configure Modulith Outbox and event externalization in new service
  - File: orders/pom.xml, orders/src/main/resources/application.properties
  - Add spring-modulith-events dependencies and enable JDBC outbox with schema initialization and replay on restart
  - Configure externalized routing (exchange/topic) matching monolith for seamless event flow
  - Purpose: Ensure reliable, transactional event delivery from the extracted service
  - _Leverage: src/main/resources/application.properties, src/main/java/com/sivalabs/bookstore/orders/domain/models/OrderCreatedEvent.java_
  - _Requirements: 2.2, 2.4_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Eventing Engineer with Outbox pattern experience | Task: Configure Modulith event externalization and JDBC outbox in the new orders service matching existing routing | Restrictions: Keep routing keys stable; ensure idempotent republish on restart | _Leverage: existing modulith events properties and event definitions_ | _Requirements: 2.2, 2.4_ | Success: Events persist to outbox and are published to RabbitMQ with correct routing | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [x] 13. Add RabbitMQ topology for new service (exchange/queue/DLX)
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/config/RabbitMQConfig.java
  - Replicate exchange, queue, DLX/DLQ bindings for orders events with feature-flag binding control
  - Ensure message converter uses Jackson2JsonMessageConverter
  - Purpose: Provide robust messaging topology consistent with monolith
  - _Leverage: src/main/java/com/sivalabs/bookstore/config/RabbitMQConfig.java_
  - _Requirements: 2.2_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Messaging Engineer with RabbitMQ configuration expertise | Task: Create RabbitMQ topology for the new service mirroring monolith setup | Restrictions: Keep names consistent; make binding optional via property | _Leverage: existing RabbitMQConfig_ | _Requirements: 2.2_ | Success: New service boots with exchange/queue/DLQ configured and converter set | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

## Phase 6: Code Migration and Service Implementation

- [x] 14. Migrate core domain entities and value objects
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/domain/OrderEntity.java, domain/models/
  - Copy and adapt all domain entities, value objects, and domain events
  - Ensure proper JPA configuration for independent database operation
  - Purpose: Establish complete domain model in the new microservice
  - _Leverage: existing domain entities OrderEntity, Customer, OrderItem, OrderStatus, OrderCreatedEvent_
  - _Requirements: 1.2, 5.1, 5.2_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Domain Modeling Expert with JPA and Spring Modulith events expertise | Task: Migrate core domain entities and value objects following requirements 1.2, 5.1, 5.2, adapting for independent database while preserving domain logic | Restrictions: Must preserve exact domain logic, maintain event schema compatibility, adapt only infrastructure concerns | _Leverage: existing domain entities OrderEntity, Customer, OrderItem, OrderStatus, OrderCreatedEvent_ | _Requirements: 1.2, 5.1, 5.2_ | Success: Domain model migrated with full functionality, JPA configuration operational, event schema preserved, domain logic intact | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [x] 15. Implement HTTP client for external Catalog API integration
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/infrastructure/CatalogServiceClient.java
  - Create HTTP REST client to replace in-process ProductServiceClient calls
  - Implement resilience patterns: circuit breaker, retry logic, timeouts
  - Purpose: Enable product validation through external HTTP API with resilience
  - _Leverage: existing ProductServiceClient logic and validation requirements_
  - _Requirements: 4.1, 4.2, 4.3, 4.4_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Integration Developer with HTTP client and resilience pattern expertise | Task: Implement HTTP client for Catalog API following requirements 4.1, 4.2, 4.3, 4.4, replacing in-process calls with resilient HTTP integration | Restrictions: Must maintain exact validation logic, implement proper error handling, use circuit breaker and retry patterns | _Leverage: existing ProductServiceClient logic and validation requirements_ | _Requirements: 4.1, 4.2, 4.3, 4.4_ | Success: HTTP client implemented with resilience patterns, product validation functional, circuit breaker operational, error handling complete | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [x] 16. Migrate business service layer with caching integration
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/domain/OrderService.java, cache infrastructure
  - Copy core OrderService business logic and integrate caching infrastructure
  - Ensure transaction management and event publishing work correctly
  - Purpose: Implement complete business logic with performance optimizations
  - _Leverage: existing OrderService, OrderCacheService, OrderMapStore_
  - _Requirements: 1.2, 6.1, 6.2, 6.4, 6.5_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Senior Spring Developer with business logic, caching, and transaction management expertise | Task: Migrate business service layer following requirements 1.2, 6.1, 6.2, 6.4, 6.5, implementing OrderService with caching and transaction support | Restrictions: Must preserve business logic integrity, maintain transactional behavior, ensure cache consistency and circuit breaker functionality | _Leverage: existing OrderService, OrderCacheService, OrderMapStore_ | _Requirements: 1.2, 6.1, 6.2, 6.4, 6.5_ | Success: Business service migrated with complete functionality, caching operational, transactions work correctly, event publishing functional | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [x] 17. Migrate data access layer and repository infrastructure
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/domain/OrderRepository.java
  - Copy repository interfaces and ensure proper JPA configuration
  - Validate all query methods work with independent database
  - Purpose: Establish complete data access layer for the microservice
  - _Leverage: existing OrderRepository interface and JPA configuration_
  - _Requirements: 3.1, 3.5_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring Data JPA Specialist with repository pattern expertise | Task: Migrate data access layer following requirements 3.1, 3.5, ensuring OrderRepository works with independent database configuration | Restrictions: Must maintain exact repository contracts, preserve query methods, ensure JPA configuration compatibility | _Leverage: existing OrderRepository interface and JPA configuration_ | _Requirements: 3.1, 3.5_ | Success: Repository layer migrated successfully, all queries functional, JPA configuration operational, data access working correctly | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

## Phase 7: API and Web Layer Migration

- [x] 18. Migrate REST API controllers and DTOs
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/web/OrderRestController.java, DTOs
  - Copy REST controllers and data transfer objects maintaining API contracts
  - Ensure all endpoints work with new service infrastructure
  - Purpose: Maintain REST API compatibility and functionality
  - _Leverage: existing OrderRestController, OrderDto, CreateOrderRequest, CreateOrderResponse_
  - _Requirements: 1.4, 1.5_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring MVC Expert with REST API design expertise | Task: Migrate REST API controllers and DTOs following requirements 1.4, 1.5, maintaining API contracts and endpoint functionality | Restrictions: Must preserve exact API contracts, maintain request/response formats, ensure endpoint behavior consistency | _Leverage: existing OrderRestController, OrderDto, CreateOrderRequest, CreateOrderResponse_ | _Requirements: 1.4, 1.5_ | Success: REST API migrated with complete functionality, all endpoints operational, API contracts preserved, response formats consistent | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [ ] 19. Generate OpenAPI specification for new service
  - File: orders/pom.xml, orders/src/main/resources/application.properties
  - Add springdoc-openapi dependency and expose OpenAPI at standard endpoint
  - Ensure schemas match existing request/response DTOs
  - Purpose: Publish precise API contract for consumers and contract tests
  - _Leverage: existing DTOs and controllers_
  - _Requirements: 1.5_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: API Documentation Engineer with SpringDoc experience | Task: Configure OpenAPI generation for the orders service with accurate schemas | Restrictions: Do not change endpoint contracts; document as-is | _Leverage: existing controllers and DTOs_ | _Requirements: 1.5_ | Success: OpenAPI UI/JSON available; schema reflects DTOs accurately | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [ ] 20. Migrate web UI controllers and templates
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/web/OrderWebController.java, templates
  - Copy web controllers, cart functionality, and Thymeleaf templates
  - Ensure web UI maintains complete user experience
  - Purpose: Preserve web interface functionality and user experience
  - _Leverage: existing OrderWebController, CartController, Thymeleaf templates_
  - _Requirements: 1.5_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring MVC and Thymeleaf Expert with web UI expertise | Task: Migrate web UI controllers and templates following requirement 1.5, maintaining complete user interface functionality | Restrictions: Must preserve UI behavior, maintain template structure, ensure cart functionality works correctly | _Leverage: existing OrderWebController, CartController, Thymeleaf templates_ | _Requirements: 1.5_ | Success: Web UI migrated with complete functionality, templates render correctly, cart operations functional, user experience maintained | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [ ] 21. Implement exception handling and error responses
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/web/OrdersExceptionHandler.java, exception classes
  - Copy exception handling infrastructure and error response formatting
  - Ensure proper error propagation and user-friendly error messages
  - Purpose: Maintain consistent error handling and user experience
  - _Leverage: existing OrdersExceptionHandler, InvalidOrderException, OrderNotFoundException_
  - _Requirements: 1.2, 1.5_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Error Handling Specialist with Spring exception management expertise | Task: Implement exception handling following requirements 1.2, 1.5, maintaining error response consistency and user-friendly messages | Restrictions: Must preserve error handling behavior, maintain exception hierarchy, ensure proper error propagation | _Leverage: existing OrdersExceptionHandler, InvalidOrderException, OrderNotFoundException_ | _Requirements: 1.2, 1.5_ | Success: Exception handling migrated completely, error responses consistent, exception propagation functional, user messages preserved | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

## Phase 8: Testing and Quality Assurance

- [ ] 22. Migrate and adapt unit tests
  - File: orders/src/test/java/com/sivalabs/bookstore/orders/cache/, web/, domain/
  - Copy existing unit tests and adapt for microservice testing environment
  - Ensure test coverage is maintained and all tests pass
  - Purpose: Maintain code quality and regression protection
  - _Leverage: existing unit tests for cache, web controllers, and domain logic_
  - _Requirements: 7.1, 7.4, 7.5_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Test Developer with Spring Boot testing and unit test expertise | Task: Migrate unit tests following requirements 7.1, 7.4, 7.5, adapting for microservice environment while maintaining test coverage | Restrictions: Must maintain test coverage levels, preserve test scenarios, adapt only configuration for independent testing | _Leverage: existing unit tests for cache, web controllers, and domain logic_ | _Requirements: 7.1, 7.4, 7.5_ | Success: Unit tests migrated and functional, test coverage maintained, all tests pass in microservice environment | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [ ] 23. Create comprehensive integration tests with TestContainers
  - File: orders/src/test/java/com/sivalabs/bookstore/orders/OrdersIntegrationTests.java, test configuration
  - Create integration tests using TestContainers for PostgreSQL and RabbitMQ
  - Test complete application functionality including database, caching, and messaging
  - Purpose: Ensure complete system integration and functionality validation
  - _Leverage: existing integration test patterns and TestContainers setup_
  - _Requirements: 7.2, 7.3_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Integration Test Specialist with TestContainers and Spring Boot testing expertise | Task: Create integration tests following requirements 7.2, 7.3, using TestContainers for complete system validation | Restrictions: Must test complete application stack, use TestContainers for realistic integration testing, validate all major functionality | _Leverage: existing integration test patterns and TestContainers setup_ | _Requirements: 7.2, 7.3_ | Success: Integration tests created with TestContainers, complete application tested, all major functionality validated | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [ ] 24. Add REST contract tests (consumer-driven)
  - File: orders/pom.xml, orders/src/test/java/com/sivalabs/bookstore/orders/contract/
  - Configure Spring Cloud Contract (or Pact) for critical REST endpoints used by consumers
  - Validate request/response shape and backward compatibility
  - Purpose: Guard API contracts during and after extraction
  - _Leverage: OpenAPI, existing controller tests_
  - _Requirements: 1.5, 2.1_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Contract Testing Engineer | Task: Add consumer-driven contract tests for orders REST API endpoints | Restrictions: Keep contracts aligned with current API; fail on breaking changes | _Leverage: generated OpenAPI and controller tests_ | _Requirements: 1.5, 2.1_ | Success: Contract tests pass; breaking changes detected in CI | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [ ] 25. Add event schema validation tests
  - File: orders/src/test/java/com/sivalabs/bookstore/orders/events/
  - Validate OrderCreatedEvent payload schema and routing against expectations
  - Use embedded broker or mocks to assert externalized event shape
  - Purpose: Ensure event compatibility for subscribers
  - _Leverage: orders/api/events/OrderCreatedEvent, RabbitMQ configuration_
  - _Requirements: 2.2, 2.4_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Event QA Engineer | Task: Add tests that verify event payload schema and routing | Restrictions: Do not couple tests to internal implementation; assert contract-level details | _Leverage: event class and messaging config_ | _Requirements: 2.2, 2.4_ | Success: Tests confirm schema/routing; CI prevents incompatible changes | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

## Phase 9: Deployment and Operations Setup

- [ ] 26. Create containerization and deployment configurations
  - File: orders/Dockerfile, orders/docker-compose.yml, orders/k8s/
  - Create Docker containerization and orchestration configurations
  - Set up independent deployment with proper service dependencies
  - Purpose: Enable independent deployment and scaling of the microservice
  - _Leverage: existing containerization patterns and deployment configurations_
  - _Requirements: 2.1_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: DevOps Engineer with Docker and Kubernetes deployment expertise | Task: Create containerization configurations following requirement 2.1, enabling independent deployment with proper service dependencies | Restrictions: Must use independent ports and service names, configure proper networking, avoid conflicts with existing deployments | _Leverage: existing containerization patterns and deployment configurations_ | _Requirements: 2.1_ | Success: Containerization complete, Docker builds successfully, deployment configurations functional, services deploy independently | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [ ] 27. Set up monitoring and observability infrastructure
  - File: orders/src/main/resources/application.properties, monitoring configuration
  - Configure Spring Actuator, metrics, health checks, and logging
  - Ensure proper observability for the independent microservice
  - Purpose: Provide operational visibility and monitoring capabilities
  - _Leverage: existing Spring Actuator and monitoring configurations_
  - _Requirements: 2.1, 2.3_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Site Reliability Engineer with Spring Actuator and monitoring expertise | Task: Set up monitoring infrastructure following requirements 2.1, 2.3, configuring observability for independent microservice operation | Restrictions: Must configure independent monitoring endpoints, ensure health checks work correctly, maintain monitoring consistency | _Leverage: existing Spring Actuator and monitoring configurations_ | _Requirements: 2.1, 2.3_ | Success: Monitoring configured completely, health checks operational, metrics collection functional, logging properly configured | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

### Phase 10: Additions

- [ ] 28. Add orders service to docker-compose for local integration
  - File: compose.yml
  - Add orders-service container with environment config, dependencies on postgres/rabbitmq, and network wiring
  - Expose port and healthcheck; reuse Zipkin tracing if applicable
  - Purpose: Enable end-to-end local testing with the extracted service
  - _Leverage: existing compose services (postgres, rabbitmq, zipkin)_
  - _Requirements: 2.1_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Developer Experience Engineer | Task: Extend docker-compose to run the new orders service locally with dependencies | Restrictions: Keep names and networks consistent; avoid breaking existing monolith setup | _Leverage: compose.yml_ | _Requirements: 2.1_ | Success: docker-compose up runs orders service along with infra; healthchecks pass | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [ ] 29. Create CI pipeline for orders service
  - File: orders/.github/workflows/build.yml
  - Build, test (unit/integration/contract), and publish image for orders service
  - Cache Maven deps; run spotless/format and verify tasks
  - Purpose: Provide independent CI for the extracted service
  - _Leverage: existing CI patterns in repository_
  - _Requirements: 2.1_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: CI Engineer | Task: Add GitHub Actions workflow for the orders service with build/test/publish steps | Restrictions: Keep workflow minimal and reproducible; no secrets committed | _Leverage: existing build commands and Taskfile targets_ | _Requirements: 2.1_ | Success: Workflow runs on PR and main; artifacts/images produced | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

## Phase 11: Migration Validation and Cutover

- [ ] 30. Perform comprehensive end-to-end testing
  - File: Complete application testing and validation
  - Test all functionality: web UI, REST API, database operations, caching, messaging
  - Validate performance characteristics and error handling scenarios
  - Purpose: Ensure complete functionality before production cutover
  - _Leverage: all migrated components and testing infrastructure_
  - _Requirements: All requirements_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Quality Assurance Engineer with end-to-end testing and system validation expertise | Task: Perform comprehensive testing covering all requirements, validating complete functionality and performance characteristics | Restrictions: Must test all major functionality, validate performance requirements, ensure error handling works correctly | _Leverage: all migrated components and testing infrastructure_ | _Requirements: All requirements_ | Success: Complete functionality validated, performance requirements met, error handling operational, system ready for production | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [ ] 31. Implement gradual traffic migration strategy
  - File: traffic routing configuration, feature flags, monitoring setup
  - Set up feature flags or API gateway for gradual traffic migration
  - Implement monitoring to compare old vs new service performance
  - Purpose: Enable safe, gradual migration with rollback capability
  - _Leverage: feature flag patterns and traffic routing strategies_
  - _Requirements: All requirements_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Migration Specialist with feature flag and traffic routing expertise | Task: Implement gradual migration strategy covering all requirements, setting up traffic routing and monitoring for safe cutover | Restrictions: Must ensure rollback capability, implement proper monitoring, enable gradual traffic shifting | _Leverage: feature flag patterns and traffic routing strategies_ | _Requirements: All requirements_ | Success: Migration strategy implemented, traffic routing functional, monitoring operational, rollback capability confirmed | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [ ] 32. Execute production cutover and validation
  - File: production deployment and validation procedures
  - Execute the actual migration with monitoring and validation
  - Validate production functionality and performance metrics
  - Purpose: Complete the migration with full production validation
  - _Leverage: all migration infrastructure and monitoring systems_
  - _Requirements: All requirements_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Production Migration Specialist with deployment and validation expertise | Task: Execute production cutover covering all requirements, validating functionality and performance in production environment | Restrictions: Must monitor all metrics carefully, validate complete functionality, ensure rollback readiness | _Leverage: all migration infrastructure and monitoring systems_ | _Requirements: All requirements_ | Success: Production migration completed successfully, all functionality operational, performance metrics within targets, monitoring confirms successful cutover | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

### Phase 12 Additions

- [ ] 33. Implement initial data backfill job for orders
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/migration/BackfillRunner.java, orders/src/main/resources/db/migration/
  - Create a runnable backfill job to copy existing orders into the new service database (configurable window/limit)
  - Add SQL migration or tooling to support initial schema prep for backfill
  - Purpose: Prepare independent data store with historical orders prior to cutover
  - _Leverage: existing JPA entities and Liquibase/Flyway patterns_
  - _Requirements: 3.1, 3.5_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Data Migration Engineer | Task: Implement an initial backfill job for orders migrating data into the new DB | Restrictions: Make job idempotent and chunked; do not impact production workload | _Leverage: entity mappings and DB change management_ | _Requirements: 3.1, 3.5_ | Success: Backfill runs successfully on sample data; verifies counts and spot-checks integrity | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_

- [ ] 34. Add backfill validation and rollback script
  - File: orders/src/test/java/com/sivalabs/bookstore/orders/migration/BackfillTests.java, orders/scripts/rollback.sql
  - Write tests validating record counts and key invariants after backfill; provide rollback script for partial migrations
  - Purpose: Ensure safe migration with validation and recovery path
  - _Leverage: TestContainers for DB integration_
  - _Requirements: 3.1, 3.5_
  - _Prompt: Implement the task for spec order-module-extraction, first run spec-workflow-guide to get the workflow guide then implement the task: Role: QA/Data Reliability Engineer | Task: Add tests and rollback resources for the backfill process | Restrictions: Keep scripts environment-agnostic; do not affect production DBs during tests | _Leverage: integration test setup_ | _Requirements: 3.1, 3.5_ | Success: Tests pass and rollback script validated against a test DB | Instructions: Mark this task as in-progress in tasks.md before starting, mark as complete when finished_
