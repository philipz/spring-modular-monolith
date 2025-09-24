# Implementation Plan - Monolith Orders gRPC Migration

## Task Overview
Implementation approach follows the design document by creating Protocol Buffer schemas, implementing gRPC server and client components, and integrating with existing Spring Boot patterns. Tasks are organized to maintain existing business logic while adding gRPC communication layer.

## Steering Document Compliance
Tasks follow Spring Boot project structure conventions:
- Protocol Buffer files in `src/main/proto/` following Maven standards
- gRPC implementations in `infrastructure/grpc/` packages within respective modules
- Configuration classes alongside existing patterns in `config/` packages
- Generated code in `target/generated-sources/protobuf/java/`

## Atomic Task Requirements
**Each task meets the following criteria for optimal agent execution:**
- **File Scope**: Touches 1-3 related files maximum
- **Time Boxing**: Completable in 15-30 minutes
- **Single Purpose**: One testable outcome per task
- **Specific Files**: Must specify exact files to create/modify
- **Agent-Friendly**: Clear input/output with minimal context switching

## Task Format Guidelines
- Use checkbox format: `- [ ] Task number. Task description`
- **Specify files**: Always include exact file paths to create/modify
- **Include implementation details** as bullet points
- Reference requirements using: `_Requirements: X.Y, Z.A_`
- Reference existing code to leverage using: `_Leverage: path/to/file.java_`
- Focus only on coding tasks (no deployment, user testing, etc.)
- **Avoid broad terms**: No "system", "integration", "complete" in task titles

## Good vs Bad Task Examples
❌ **Bad Examples (Too Broad)**:
- "Implement gRPC system" (affects many files, multiple purposes)
- "Add gRPC integration features" (vague scope, no file specification)
- "Build complete gRPC server" (too large, multiple components)

✅ **Good Examples (Atomic)**:
- "Create OrdersService.proto file with CreateOrder RPC method definition"
- "Add GrpcServerProperties configuration class in orders/config/ package"
- "Create OrdersGrpcService class delegating to existing OrdersApiService"

## Tasks

### Phase 1: Maven Configuration and Protocol Buffer Setup

- [x] 1. Add gRPC Maven dependencies to root pom.xml
  - File: pom.xml
  - Add net.devh:grpc-spring-boot-starter:2.15.0.RELEASE dependency
  - Add io.grpc:grpc-protobuf:1.58.0 and io.grpc:grpc-stub:1.58.0 dependencies
  - Add protobuf-maven-plugin:0.6.1 with protoc:3.24.0 configuration
  - Purpose: Enable gRPC and Protocol Buffer code generation in Maven build
  - _Requirements: 1.1, 5.1_

- [x] 2. Create Orders Protocol Buffer schema file
  - File: src/main/proto/orders.proto
  - Define OrdersService with CreateOrder, FindOrder, FindOrders RPC methods
  - Create message types: CreateOrderRequest, CreateOrderResponse, OrderDto, OrderView
  - Define nested Customer, OrderItem, OrderStatus messages with proper field numbers
  - Purpose: Establish strongly-typed gRPC service contract
  - _Requirements: 1.1, 1.2_

- [x] 3. Create Product Catalog Protocol Buffer schema file
  - File: src/main/proto/catalog.proto
  - Define ProductCatalogService with ValidateProduct RPC method
  - Create ProductValidationRequest and ProductValidationResponse messages
  - Purpose: Enable gRPC communication for product validation
  - _Requirements: 4.1_

### Phase 2: gRPC Server Configuration

- [x] 4. Create gRPC server configuration properties class
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/config/GrpcServerProperties.java
  - Define @ConfigurationProperties with port, message size, keep-alive, and security settings
  - Include nested SecurityConfig record for TLS configuration
  - Purpose: Externalize gRPC server configuration following Spring Boot patterns
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/infrastructure/catalog/ProductApiProperties.java_
  - _Requirements: 5.1, 5.2_

- [x] 5. Create gRPC server configuration class
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/config/GrpcServerConfiguration.java
  - Configure gRPC server startup with Spring Boot integration using GrpcServerProperties
  - Create NettyServerBuilder bean with port and message size configuration
  - Purpose: Bootstrap gRPC server with basic Spring integration
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/config/ProductClientConfiguration.java_
  - _Requirements: 2.4, 5.2_

- [x] 5a. Add server interceptor registration to GrpcServerConfiguration
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/config/GrpcServerConfiguration.java (continue from task 5)
  - Register server interceptors for metrics collection and authentication
  - Enable server reflection for development and testing
  - Purpose: Add server interceptor support to gRPC configuration
  - _Requirements: 6.1, 6.2_

- [x] 6. Create gRPC exception mapper component
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/infrastructure/grpc/GrpcExceptionMapper.java
  - Map OrderNotFoundException to Status.NOT_FOUND with message preservation
  - Map InvalidOrderException to Status.INVALID_ARGUMENT with validation details
  - Map CatalogServiceException to Status.UNAVAILABLE with retry hints
  - Purpose: Consistent exception translation between Spring and gRPC patterns
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/OrderNotFoundException.java, orders/src/main/java/com/sivalabs/bookstore/orders/InvalidOrderException.java_
  - _Requirements: 2.3, 3.4_

### Phase 3: gRPC Server Implementation

- [x] 7. Create Protocol Buffer to domain object mapper
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/infrastructure/grpc/GrpcOrderMapper.java
  - Convert Proto CreateOrderRequest to domain CreateOrderRequest
  - Convert domain OrderDto to Proto OrderDto with proper type conversions (BigDecimal → double)
  - Convert domain OrderView list to Proto OrderView repeated field
  - Purpose: Isolate Protocol Buffer generated classes from business domain
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/mappers/OrderMapper.java_
  - _Requirements: 1.3, 2.2_

- [x] 8. Create Orders gRPC service implementation
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/infrastructure/grpc/OrdersGrpcService.java
  - Extend OrdersServiceGrpc.OrdersServiceImplBase from generated code
  - Delegate createOrder RPC to existing OrdersApiService.createOrder method
  - Delegate findOrder RPC to existing OrdersApiService.findOrder method
  - Delegate findOrders RPC to existing OrdersApiService.findOrders method
  - Purpose: Expose gRPC interface while reusing existing business logic
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/OrdersApiService.java_
  - _Requirements: 2.1, 2.2_

- [ ] 9. Add gRPC service error handling to OrdersGrpcService
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/infrastructure/grpc/OrdersGrpcService.java (continue from task 8)
  - Wrap all delegated calls with try-catch blocks
  - Use GrpcExceptionMapper to convert exceptions to gRPC StatusRuntimeException
  - Include original error messages and context in gRPC status descriptions
  - Purpose: Provide consistent error experience between HTTP and gRPC APIs
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/infrastructure/grpc/GrpcExceptionMapper.java_
  - _Requirements: 2.3, 3.4_

### Phase 4: gRPC Client Configuration

- [ ] 10. Create gRPC client configuration properties class
  - File: src/main/java/com/sivalabs/bookstore/config/GrpcClientProperties.java
  - Define @ConfigurationProperties with server address, timeouts, and retry settings
  - Include nested RetryConfig and SecurityConfig records
  - Purpose: Configure gRPC client connections with externalized properties
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/infrastructure/catalog/ProductApiProperties.java_
  - _Requirements: 3.2, 5.1_

- [ ] 11. Create gRPC client configuration class
  - File: src/main/java/com/sivalabs/bookstore/config/GrpcClientConfiguration.java
  - Create OrdersServiceGrpc.OrdersServiceStub bean with ManagedChannel
  - Configure channel builder with address, keep-alive, and timeout settings
  - Purpose: Provide configured gRPC client channel and stub for dependency injection
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/infrastructure/catalog/ProductClientConfiguration.java_
  - _Requirements: 3.1, 3.2_

- [ ] 11a. Add client interceptors to GrpcClientConfiguration
  - File: src/main/java/com/sivalabs/bookstore/config/GrpcClientConfiguration.java (continue from task 11)
  - Apply client interceptors for metrics collection and distributed tracing
  - Configure retry and circuit breaker interceptors using Resilience4j
  - Purpose: Add observability and resilience to gRPC client calls
  - _Requirements: 3.3, 6.2_

- [ ] 12. Create Orders gRPC client implementation
  - File: src/main/java/com/sivalabs/bookstore/orders/client/OrdersGrpcClient.java
  - Implement methods wrapping OrdersServiceStub gRPC calls
  - Apply @CircuitBreaker and @Retry annotations for resilience
  - Convert gRPC responses back to domain objects using mapper
  - Purpose: Provide gRPC client with same interface patterns as HTTP clients
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/infrastructure/catalog/HttpProductCatalogClient.java_
  - _Requirements: 3.1, 3.3_

### Phase 5: Product Catalog gRPC Integration

- [ ] 13. Create Product Catalog gRPC client implementation
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/infrastructure/catalog/GrpcProductCatalogClient.java
  - Implement ProductCatalogPort interface using gRPC ProductCatalogService
  - Reuse existing validation logic with price tolerance from HttpProductCatalogClient
  - Apply same @CircuitBreaker and @Retry configuration as HTTP client
  - Purpose: Replace HTTP product validation with gRPC while maintaining interface contract
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/infrastructure/catalog/HttpProductCatalogClient.java, orders/src/main/java/com/sivalabs/bookstore/orders/domain/ProductCatalogPort.java_
  - _Requirements: 4.1, 4.2, 4.3_

- [ ] 14. Create Catalog gRPC service implementation for product validation
  - File: src/main/java/com/sivalabs/bookstore/catalog/infrastructure/grpc/CatalogGrpcService.java
  - Extend ProductCatalogServiceGrpc.ProductCatalogServiceImplBase
  - Delegate validateProduct RPC to existing ProductApi service
  - Convert catalog domain objects to Proto messages
  - Purpose: Expose product validation via gRPC for Orders module consumption
  - _Leverage: src/main/java/com/sivalabs/bookstore/catalog/ProductApi.java_
  - _Requirements: 4.1, 4.4_

### Phase 6: Configuration Integration and Health Checks

- [ ] 15. Add gRPC configuration to application.properties
  - File: src/main/resources/application.properties
  - Add grpc.server.port=9090 and other server configuration properties
  - Add grpc.client.orders.address=localhost:9090 and client settings
  - Include development, test, and production profile examples
  - Purpose: Externalize gRPC configuration following Spring Boot conventions
  - _Leverage: existing application properties patterns_
  - _Requirements: 5.2, 5.3_

- [ ] 16. Create gRPC health indicator component
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/config/GrpcHealthIndicator.java
  - Implement HealthIndicator interface with gRPC service connectivity check
  - Perform health check RPC call with timeout to validate service availability
  - Return Health.up/down with service status details
  - Purpose: Integrate gRPC service health with Spring Boot Actuator
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/config/CacheHealthIndicator.java_
  - _Requirements: 6.1, 6.3_

- [ ] 17. Create gRPC metrics configuration
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/config/GrpcMetricsConfiguration.java
  - Create @Configuration class with MeterRegistry dependency
  - Define server interceptor bean for gRPC request metrics collection
  - Register Timer and Counter metrics for request duration and error rates
  - Purpose: Monitor gRPC server performance with Micrometer integration
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/config/CacheMetricsConfig.java_
  - _Requirements: 6.2_

- [ ] 17a. Add client metrics interceptor to GrpcMetricsConfiguration
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/config/GrpcMetricsConfiguration.java (continue from task 17)
  - Define client interceptor bean for outgoing gRPC call metrics
  - Add proper metric tags for service, method, and status code dimensions
  - Purpose: Monitor gRPC client performance and throughput
  - _Requirements: 6.2_

### Phase 7: Testing and Validation

- [ ] 18. Create gRPC service unit test setup
  - File: orders/src/test/java/com/sivalabs/bookstore/orders/infrastructure/grpc/OrdersGrpcServiceTest.java
  - Create test class with @ExtendWith(MockitoExtension.class) and mocked dependencies
  - Mock OrdersApiService and GrpcExceptionMapper dependencies
  - Setup test fixtures for CreateOrderRequest and OrderDto objects
  - Purpose: Prepare unit test infrastructure for gRPC service testing
  - _Leverage: orders/src/test/java/com/sivalabs/bookstore/orders/web/OrderRestControllerTests.java_
  - _Requirements: 2.1_

- [ ] 18a. Add gRPC service createOrder test cases
  - File: orders/src/test/java/com/sivalabs/bookstore/orders/infrastructure/grpc/OrdersGrpcServiceTest.java (continue from task 18)
  - Test successful createOrder RPC with mocked OrdersApiService response
  - Test createOrder with InvalidOrderException and verify Status.INVALID_ARGUMENT conversion
  - Purpose: Validate createOrder RPC behavior and exception mapping
  - _Requirements: 2.1, 2.3_

- [ ] 18b. Add gRPC service findOrder test cases
  - File: orders/src/test/java/com/sivalabs/bookstore/orders/infrastructure/grpc/OrdersGrpcServiceTest.java (continue from task 18a)
  - Test successful findOrder RPC with OrderDto response conversion
  - Test findOrder with OrderNotFoundException and verify Status.NOT_FOUND conversion
  - Purpose: Validate findOrder RPC behavior and error handling
  - _Requirements: 2.1, 2.3_

- [ ] 19. Create gRPC client integration test setup
  - File: src/test/java/com/sivalabs/bookstore/orders/client/OrdersGrpcClientIntegrationTest.java
  - Create @SpringBootTest with @GrpcTest annotation for embedded server
  - Configure test properties for gRPC client and server settings
  - Setup test data and GrpcCleanupRule for proper resource cleanup
  - Purpose: Prepare integration test infrastructure for client-server testing
  - _Requirements: 3.1_

- [ ] 19a. Add gRPC client-server roundtrip tests
  - File: src/test/java/com/sivalabs/bookstore/orders/client/OrdersGrpcClientIntegrationTest.java (continue from task 19)
  - Test full createOrder client-server roundtrip with Proto message serialization
  - Test findOrder and findOrders operations end-to-end
  - Purpose: Validate complete gRPC communication flow
  - _Requirements: 3.1, 3.3_

- [ ] 20. Create Protocol Buffer schema compatibility tests
  - File: src/test/java/com/sivalabs/bookstore/orders/proto/SchemaCompatibilityTest.java
  - Test backward compatibility between Proto message versions
  - Validate optional field handling and default value behavior
  - Test message parsing with missing and additional fields
  - Purpose: Ensure schema evolution doesn't break existing clients
  - _Requirements: 7.1, 7.4_
