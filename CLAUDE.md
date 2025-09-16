# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot modular monolith e-commerce application demonstrating Spring Modulith features. The application is built around a bookstore domain with clearly separated business modules that communicate through events and well-defined APIs.

## Development Commands

### Build and Test
```bash
# Run tests (includes formatting check)
task test

# Or using Maven directly
./mvnw clean verify

# Format code using Spotless
task format
# or
./mvnw spotless:apply

# Build without tests
./mvnw clean compile
```

### Running the Application

#### Local Development (with Docker services)
```bash
# Build Docker image and start all services
task start

# Stop services
task stop

# Restart services
task restart

# Run just the application (requires external PostgreSQL/RabbitMQ)
./mvnw spring-boot:run
```

#### Kubernetes Development
```bash
# Create KinD cluster
task kind_create

# Deploy to cluster
task k8s_deploy

# Clean up
task k8s_undeploy
task kind_destroy
```

### Testing

#### Run All Tests
```bash
task test
```

#### Run Specific Test Classes
```bash
# Module-specific tests
./mvnw test -Dtest=CatalogIntegrationTests
./mvnw test -Dtest=OrdersIntegrationTests
./mvnw test -Dtest=InventoryIntegrationTests

# Modularity verification
./mvnw test -Dtest=ModularityTests

# REST API tests
./mvnw test -Dtest=ProductRestControllerTests
./mvnw test -Dtest=OrderRestControllerTests

# Cache integration tests
./mvnw test -Dtest=ProductCacheServiceIntegrationTests
./mvnw test -Dtest=InventoryCacheServiceIntegrationTests
./mvnw test -Dtest=HazelcastConfigTests

# Cache unit tests
./mvnw test -Dtest=OrderCacheServiceTests
./mvnw test -Dtest=ProductMapStoreTests
./mvnw test -Dtest=InventoryMapStoreTests
```

#### Integration Tests
```bash
# Run all integration tests
./mvnw verify -Dit.test="**/*IntegrationTests"

# Run cache-specific integration tests
./mvnw test -Dtest="**/*CacheService*IntegrationTests"
./mvnw test -Dtest="**/*MapStore*Tests"
```

## Architecture Overview

### Module Structure
The application follows Spring Modulith principles with these business modules:

- **Common**: Shared code and utilities (open module)
- **Catalog**: Product catalog management (stores in `catalog` schema)
- **Orders**: Order management (stores in `orders` schema)  
- **Inventory**: Stock management (stores in `inventory` schema)
- **Notifications**: Event-driven notifications

### Module Communication Patterns

#### API-based Communication
- Orders module calls Catalog module's public API (`ProductApi`) for product validation
- Each module exposes a public API component (e.g., `ProductApi`, `OrderApi`)

#### Event-driven Communication
- Orders publishes `OrderCreatedEvent` when order is successfully created
- Events are published to both internal Spring Modulith event bus and external RabbitMQ
- Inventory module consumes `OrderCreatedEvent` to update stock levels
- Notifications module consumes `OrderCreatedEvent` to send confirmation emails

#### Caching Architecture
- **Module-specific cache services**: Each module has its own cache service layer
- **Shared cache infrastructure**: Common CacheErrorHandler and circuit breaker
- **Write-through pattern**: Cache automatically syncs with database via MapStore
- **Cache isolation**: Each module's cache operates independently
- **Cross-module health monitoring**: Unified health reporting across all caches

#### Data Isolation
- Each module manages its own database schema
- No direct cross-module database access
- PostgreSQL with separate schemas per module
- **Cache data isolation**: Each module caches only its own domain entities

### Key Spring Modulith Features Used

#### Module Verification
```java
// In ModularityTests.java
ApplicationModules.of(BookStoreApplication.class).verify();
```

#### Event Publishing
```java
@Component
class OrderService {
    @EventListener
    void publishOrderCreated(OrderCreatedEvent event) {
        // Event automatically published to other modules
    }
}
```

#### Module Testing
```java
@ApplicationModuleTest
class CatalogIntegrationTests {
    // Test only loads catalog module components
}
```

## Technology Stack

### Core Framework
- **Spring Boot 3.5.5** with Java 21
- **Spring Modulith 1.4.3** for modular architecture
- **Spring Data JPA** for persistence
- **Spring AMQP** for messaging

### Caching Infrastructure
- **Hazelcast** distributed cache for write-through caching
- **Three cache types**: Orders (String keys), Products (String keys), Inventory (Long keys)
- **MapStore integration** for automatic cache-database synchronization
- **Circuit breaker pattern** for cache fault tolerance
- **Cache health monitoring** via Spring Boot Actuator

### Database & Migration
- **PostgreSQL** as primary database
- **Liquibase** for database migrations
- **Separate schemas** per module for data isolation

### Messaging & Events
- **RabbitMQ** for external event publishing
- **Spring Modulith Events** for internal module communication
- **JDBC-based event store** with automatic republishing

### Observability
- **Micrometer** with Prometheus registry
- **OpenTelemetry** tracing with Zipkin export
- **Spring Actuator** with modulith endpoints
- **Cache metrics** and health indicators

### Frontend
- **Thymeleaf** templating
- **HTMX** for dynamic interactions  
- **Bootstrap** for UI styling

### Code Quality
- **Spotless** with Palantir Java Format for code formatting
- **Enhanced test reporting** with JUnit 5 tree reporter

## Development Guidelines

### Module Design Principles
1. **Independence**: Each module should be as self-contained as possible
2. **Event-first**: Prefer event-driven communication over direct API calls
3. **Data Ownership**: Each module owns its data and schema
4. **Public APIs**: Expose functionality through dedicated API components
5. **Testability**: Modules should be testable in isolation using `@ApplicationModuleTest`

### Module Organization & Structure
- **Java sources**: `src/main/java/com/sivalabs/bookstore/{common,catalog,orders,inventory,notifications,config}`
- **Resources**: `src/main/resources/{templates,static,db}` (Liquibase migrations in `db/migration/`)
- **Tests**: `src/test/java` (module-focused tests; Testcontainers where needed)
- **Operations**: `compose.yml`, `k8s/`, and `Taskfile.yml` for local/dev workflows
- **Module Boundaries**: Define with `@ApplicationModule` (`package-info.java`) and `@NamedInterface`
- **Allowed Dependencies**: Respect module boundaries (e.g., `orders` → `catalog` only)
- **Cross-module Access**: Via explicit APIs (e.g., `catalog.ProductApi`)

### Adding New Modules
1. Create package under `com.sivalabs.bookstore.[modulename]`
2. Add `package-info.java` to define module boundaries
3. Create dedicated database schema and Liquibase migrations
4. Implement public API component for cross-module access
5. Add module-specific integration test with `@ApplicationModuleTest`
6. Update `ModularityTests` to verify new module structure

### Coding Style & Naming Conventions
- **Java Version**: Java 21 with 4-space indentation, one class per file
- **Naming**:
  - Packages: lowercase
  - Classes: PascalCase
  - Methods/fields: camelCase
  - Constants: UPPER_SNAKE_CASE
- **Formatting**: Run `task format` before pushing (Spotless + Palantir Java Format)
- **Module APIs**: Keep small; prefer event-driven communication
- **License Headers**: Avoid adding license headers

### Event Handling Best Practices
- Use `@EventListener` for consuming events within the same module
- Events are automatically published externally via RabbitMQ configuration
- Events are persisted and can be replayed on application restart
- Design events as immutable data structures (see `orders.api.events.OrderCreatedEvent`)

### Database Schema Management
- Each module manages its own Liquibase migrations in `db/migration/[module]/`
- Use module-specific schema names (e.g., `catalog`, `orders`, `inventory`)
- No cross-schema foreign keys or joins
- Seed data in `src/main/resources/db/migration` when appropriate

### Testing Guidelines
- **Frameworks**: JUnit 5, Spring Boot Test, Spring Modulith Test, Testcontainers (Postgres, RabbitMQ)
- **Naming Convention**: `*Tests.java`
- **Scope**: Test modules in isolation; avoid loading the whole app unless required
- **Data Setup**: Rely on Liquibase for schema setup
- **Execution**: `./mvnw -q -DskipITs=false test` or `./mvnw clean verify`

### Security & Configuration
- **Configuration**: Use `application.properties`; override via environment variables
- **Docker Compose**: Sets DB/RabbitMQ/Zipkin automatically
- **Secrets**: Never commit secrets; use `.env` files or CI secrets
- **Cache/Session**: Hazelcast defaults tunable via `bookstore.cache.*` properties

### Commit & Pull Request Guidelines
- **Commit Messages**: Concise, imperative mood (e.g., "Add orders cache metrics")
- **Grouping**: Group related changes in single commits
- **PR Requirements**:
  - Clear description with rationale
  - Screenshots for UI changes
  - Link related issues
  - Green build status
  - Formatted code (`task format`)
  - Added/updated tests
  - No cross-module violations

## Configuration

### Application URLs
- **Application**: http://localhost:8080
- **Actuator**: http://localhost:8080/actuator
- **Modulith Info**: http://localhost:8080/actuator/modulith
- **Cache Health**: http://localhost:8080/actuator/health
- **Cache Metrics**: http://localhost:8080/actuator/metrics
- **RabbitMQ Admin**: http://localhost:15672 (guest/guest)
- **Zipkin**: http://localhost:9411

### Key Configuration Properties
- **Database**: PostgreSQL connection via `spring.datasource.*`
- **Events**: JDBC event store with schema initialization
- **Tracing**: Full sampling with Zipkin export
- **RabbitMQ**: Local connection for event publishing
- **Cache**: Hazelcast configuration via `bookstore.cache.*`

#### Cache Configuration Properties
```properties
# Enable/disable caching
bookstore.cache.enabled=true

# Write-through cache behavior
bookstore.cache.write-through=true
bookstore.cache.write-delay-seconds=1

# Cache sizing and TTL
bookstore.cache.max-size=1000
bookstore.cache.time-to-live-seconds=3600

# Cache metrics and monitoring
bookstore.cache.metrics-enabled=true

# Circuit breaker settings
bookstore.cache.circuit-breaker.failure-threshold=5
bookstore.cache.circuit-breaker.recovery-timeout=30000
```

### Environment Setup
- **Java 21** (recommended: install via SDKMAN)
- **Docker & Docker Compose** for running dependencies
- **Task runner** (go-task) for simplified command execution
- **Maven Wrapper** included in project
