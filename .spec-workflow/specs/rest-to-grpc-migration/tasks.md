# Implementation Plan: REST to gRPC Migration

## Task Overview

This implementation plan breaks down the REST to gRPC migration into atomic, executable tasks. Each task is scoped to 1-3 files and completable in 15-30 minutes, following the design document's architecture.

## Steering Document Compliance

- **structure.md**: Tasks follow project file organization conventions
- **tech.md**: Tasks implement patterns documented in CLAUDE.md (conditional loading, properties configuration)
- **Spring Modulith**: All tasks respect module boundaries defined in `package-info.java`

## Atomic Task Requirements

Each task meets these criteria for optimal execution:
- **File Scope**: Touches 1-3 related files maximum
- **Time Boxing**: Completable in 15-30 minutes
- **Single Purpose**: One testable outcome per task
- **Specific Files**: Exact file paths to create/modify
- **Agent-Friendly**: Clear input/output with minimal context switching

## Tasks

### Phase 1: Protocol Buffer Definition & Build Setup

- [x] 1. Add gRPC and Protocol Buffers dependencies to pom.xml
  - File: `pom.xml`
  - Add gRPC dependencies (grpc-netty-shaded, grpc-protobuf, grpc-stub, grpc-services)
  - Add Protocol Buffers dependency (protobuf-java)
  - Add gRPC testing dependency for test scope
  - Set version properties for gRPC (1.65.1) and Protobuf (3.25.3)
  - _Requirements: 1.1, 2.2, 6.2_
  - _Leverage: Existing dependency management in pom.xml_

- [x] 2. Configure protobuf-maven-plugin in pom.xml
  - File: `pom.xml` (continue from task 1)
  - Add os-maven-plugin extension for platform detection
  - Configure protobuf-maven-plugin with compile and compile-custom goals
  - Set protoc artifact with version and platform classifier
  - Configure grpc-java plugin artifact
  - _Requirements: 1.1_
  - _Leverage: Existing Maven plugin configuration patterns_

- [x] 3. Create orders.proto with service and request/response messages
  - File: `src/main/proto/orders.proto`
  - Define syntax proto3 and package name com.sivalabs.bookstore.orders.grpc.proto
  - Import google/protobuf/timestamp.proto
  - Define OrdersService with createOrder, getOrder, listOrders RPC methods
  - Define CreateOrderRequest, CreateOrderResponse messages
  - Define GetOrderRequest, GetOrderResponse messages
  - Define ListOrdersRequest, ListOrdersResponse messages
  - _Requirements: 1.1, 1.2, 1.3_
  - _Leverage: Mirror structure from existing OrdersApi interface (orders.api.OrdersApi)_

- [x] 4. Add Customer and OrderItem messages to orders.proto
  - File: `src/main/proto/orders.proto` (continue from task 3)
  - Define Customer message (name, email, phone fields)
  - Define OrderItem message (code, name, price as string, quantity as int32)
  - Add documentation comments following Google protobuf style guide
  - _Requirements: 1.3, 1.4_
  - _Leverage: Map from existing Customer and OrderItem records (orders.api.model)_

- [x] 5. Add OrderDto message to orders.proto
  - File: `src/main/proto/orders.proto` (continue from task 4)
  - Define OrderDto message with order_number, item, customer, delivery_address fields
  - Add status field using OrderStatus enum (defined in next task)
  - Add created_at field using google.protobuf.Timestamp
  - Add total_amount field as string for BigDecimal precision
  - _Requirements: 1.3, 1.4, 5.4, 5.5_
  - _Leverage: Map from existing OrderDto record (orders.api.OrderDto)_

- [x] 6. Add OrderView and OrderStatus enum to orders.proto
  - File: `src/main/proto/orders.proto` (continue from task 5)
  - Define OrderView message (order_number, status fields)
  - Define OrderStatus enum with UNSPECIFIED=0, NEW=1, DELIVERED=2, CANCELLED=3, ERROR=4
  - Add proto3 style documentation comments
  - _Requirements: 1.3, 1.4_
  - _Leverage: Map from OrderView and OrderStatus (orders.api.OrderView, orders.api.model.OrderStatus)_

- [x] 7. Verify protobuf compilation with Maven build
  - Run: `./mvnw clean compile`
  - Verify generated Java classes in `target/generated-sources/protobuf/java/`
  - Verify generated gRPC stubs in `target/generated-sources/protobuf/grpc-java/`
  - Check compilation succeeds without errors
  - Verify OrdersServiceGrpc class is generated
  - _Requirements: 1.1, 1.5_
  - _Leverage: Existing Maven build process_

### Phase 2: Configuration Infrastructure

- [x] 8. Create GrpcProperties configuration class structure
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcProperties.java`
  - Add @ConfigurationProperties(prefix = "bookstore.grpc") annotation
  - Add @Validated annotation for constraint validation
  - Create nested ServerProperties class with fields (no implementation yet)
  - Create nested ClientProperties class with fields (no implementation yet)
  - _Requirements: 6.1, 6.3, 6.4_
  - _Leverage: CacheProperties pattern with nested configuration classes_

- [x] 9. Implement GrpcProperties.ServerProperties with validation
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcProperties.java` (continue from task 8)
  - Add @Min/@Max validated fields: port (1024-65535), maxInboundMessageSize
  - Add boolean fields: healthCheckEnabled, reflectionEnabled
  - Add shutdownGracePeriodSeconds with @Min(0)
  - Set default values: port=9091, maxInboundMessageSize=4194304, shutdownGracePeriodSeconds=30
  - Add getters and setters for all fields
  - _Requirements: 6.3, 6.5_
  - _Leverage: CacheProperties validation pattern_

- [x] 10. Implement GrpcProperties.ClientProperties with defaults
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcProperties.java` (continue from task 9)
  - Add string field: target with default "localhost:9091"
  - Add @Min validated fields: deadlineMs (default 5000), maxRetryAttempts (default 3)
  - Add boolean field: retryEnabled (default true)
  - Add getters and setters for all fields
  - Add toString() method for debugging
  - _Requirements: 3.1, 3.2, 3.4, 6.4_
  - _Leverage: CacheProperties default values pattern_

- [x] 11. Create GrpcServerConfig configuration class
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcServerConfig.java`
  - Add @Configuration annotation
  - Add @EnableConfigurationProperties(GrpcProperties.class)
  - Add @ConditionalOnClass(name = "io.grpc.Server") for conditional loading
  - Create empty class structure ready for bean definitions
  - _Requirements: 2.2, 6.2_
  - _Leverage: CLAUDE.md conditional loading pattern with @ConditionalOnClass and @ConditionalOnProperty_

- [x] 12. Implement grpcServer bean in GrpcServerConfig
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcServerConfig.java` (continue from task 11)
  - Create @Bean method for io.grpc.Server
  - Use ServerBuilder.forPort() with properties.getServer().getPort()
  - Add OrdersGrpcService to server (injected dependency)
  - Set maxInboundMessageSize from properties
  - Conditionally add HealthStatusManager service if enabled
  - Conditionally add ProtoReflectionService if enabled
  - Return built server instance
  - _Requirements: 2.1, 2.6, 2.7, 6.1_
  - _Leverage: Spring dependency injection pattern_

- [x] 13. Create GrpcServerLifecycle class for server lifecycle management
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcServerLifecycle.java`
  - Implement SmartLifecycle interface
  - Inject io.grpc.Server and shutdown grace period in constructor
  - Implement start() to start gRPC server and log port
  - Implement stop() with graceful shutdown using configured grace period
  - Implement isRunning() to check !server.isShutdown() && !server.isTerminated()
  - Implement getPhase() returning appropriate phase value
  - _Requirements: 2.1_
  - _Leverage: Spring SmartLifecycle interface pattern_

- [x] 14. Add GrpcServerLifecycle bean to GrpcServerConfig
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcServerConfig.java` (continue from task 12)
  - Create @Bean method for GrpcServerLifecycle
  - Inject Server and GrpcProperties as parameters
  - Pass server and properties.getServer().getShutdownGracePeriodSeconds() to constructor
  - Return lifecycle bean instance
  - _Requirements: 2.1_
  - _Leverage: Spring bean configuration pattern_

### Phase 3: Message Mapping Implementation

- [x] 15. Create GrpcMessageMapper class structure
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/GrpcMessageMapper.java`
  - Add @Component annotation
  - Add imports for generated Protobuf classes
  - Add imports for existing DTO classes (orders.api.*)
  - Create empty class ready for mapping methods
  - _Requirements: 5.1, 5.2_
  - _Leverage: Existing DTO classes from orders.api package_

- [x] 16. Implement Protobuf to DTO conversion for CreateOrderRequest
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/GrpcMessageMapper.java` (continue from task 15)
  - Implement toCreateOrderRequest(proto) method
  - Map Customer fields (name, email, phone) to Customer record
  - Map OrderItem fields with BigDecimal conversion for price
  - Handle null checks for optional fields
  - Return CreateOrderRequest DTO
  - _Requirements: 5.1, 5.3, 5.5_
  - _Leverage: Existing Customer and OrderItem record constructors_

- [x] 17. Implement DTO to Protobuf conversion for CreateOrderResponse
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/GrpcMessageMapper.java` (continue from task 16)
  - Implement toCreateOrderResponse(dtoResponse) method
  - Use CreateOrderResponse.newBuilder() pattern
  - Set orderNumber from dto.orderNumber()
  - Build and return Protobuf message
  - _Requirements: 5.2_
  - _Leverage: Protobuf builder pattern_

- [x] 18. Implement DTO to Protobuf conversion for OrderDto
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/GrpcMessageMapper.java` (continue from task 17)
  - Implement toOrderDto(dto) method converting OrderDto to Protobuf
  - Use OrderDto.newBuilder() and set all fields
  - Call helper methods toCustomerProto(), toOrderItemProto(), toOrderStatusProto()
  - Convert LocalDateTime to Timestamp using toTimestamp() helper
  - Convert BigDecimal totalAmount to string
  - _Requirements: 5.2, 5.4, 5.5_
  - _Leverage: Existing OrderDto record structure_

- [x] 19. Implement DTO to Protobuf conversion for OrderView
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/GrpcMessageMapper.java` (continue from task 18)
  - Implement toOrderView(dto) method
  - Use OrderView.newBuilder() pattern
  - Set order_number and status fields
  - Convert OrderStatus enum using toOrderStatusProto()
  - Build and return Protobuf message
  - _Requirements: 5.2_
  - _Leverage: Existing OrderView structure_

- [x] 20. Add helper methods for nested type conversions in GrpcMessageMapper
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/GrpcMessageMapper.java` (continue from task 19)
  - Create toTimestamp(LocalDateTime) converting to google.protobuf.Timestamp
  - Create toOrderStatusProto(OrderStatus) converting enum values
  - Create toCustomerProto(Customer) for nested Customer message
  - Create toOrderItemProto(OrderItem) for nested OrderItem message
  - Handle timezone conversion using ZoneId.systemDefault() for timestamps
  - _Requirements: 5.4, 5.5_
  - _Leverage: Java time API (Instant, ZoneId) and Protobuf Timestamp builder_

### Phase 4: gRPC Service Implementation

- [x] 21. Create OrdersGrpcService class extending generated base
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcService.java`
  - Add @Component annotation
  - Extend OrdersServiceGrpc.OrdersServiceImplBase
  - Inject OrdersApi and GrpcMessageMapper in constructor
  - Create empty class structure ready for RPC method implementations
  - _Requirements: 2.3, 2.4_
  - _Leverage: OrdersApi interface (orders.OrdersApiService), generated OrdersServiceGrpc base class_

- [x] 22. Implement createOrder RPC method in OrdersGrpcService
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcService.java` (continue from task 21)
  - Override createOrder(request, responseObserver) method
  - Wrap in try-catch block
  - Convert Protobuf request to DTO using messageMapper.toCreateOrderRequest()
  - Call ordersApi.createOrder(dto)
  - Convert DTO response to Protobuf using messageMapper.toCreateOrderResponse()
  - Call responseObserver.onNext(response) and responseObserver.onCompleted()
  - Catch exceptions and call responseObserver.onError(GrpcExceptionHandler.handleException(e))
  - _Requirements: 2.3, 2.4, 2.5_
  - _Leverage: Existing OrdersApi.createOrder method_

- [x] 23. Implement getOrder RPC method in OrdersGrpcService
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcService.java` (continue from task 22)
  - Override getOrder(request, responseObserver) method
  - Wrap in try-catch block
  - Extract orderNumber from request.getOrderNumber()
  - Call ordersApi.findOrder(orderNumber) returning Optional<OrderDto>
  - If empty, throw OrderNotFoundException
  - If present, convert DTO to Protobuf using messageMapper.toOrderDto()
  - Call responseObserver.onNext() and onCompleted()
  - Handle exceptions with GrpcExceptionHandler
  - _Requirements: 2.3, 2.4, 2.5_
  - _Leverage: Existing OrdersApi.findOrder method, OrderNotFoundException_

- [x] 24. Implement listOrders RPC method in OrdersGrpcService
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcService.java` (continue from task 23)
  - Override listOrders(request, responseObserver) method
  - Wrap in try-catch block
  - Call ordersApi.findOrders() to get List<OrderView>
  - Convert each OrderView to Protobuf using messageMapper.toOrderView()
  - Build ListOrdersResponse with addAllOrders(convertedList)
  - Call responseObserver.onNext(response) and onCompleted()
  - Handle exceptions with GrpcExceptionHandler
  - _Requirements: 2.3, 2.4_
  - _Leverage: Existing OrdersApi.findOrders method_

### Phase 5: Error Handling

- [x] 25. Create GrpcExceptionHandler utility class structure
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/GrpcExceptionHandler.java`
  - Create public class with package declaration
  - Add static handleException(Exception e) method signature
  - Return StatusRuntimeException type
  - Add slf4j Logger for unexpected error logging
  - Add placeholder returning Status.INTERNAL for all exceptions
  - _Requirements: 2.5_
  - _Leverage: Existing exception types (OrderNotFoundException, InvalidOrderException)_

- [x] 26. Implement exception to status code mapping in GrpcExceptionHandler
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/GrpcExceptionHandler.java` (continue from task 25)
  - Map OrderNotFoundException to Status.NOT_FOUND.withDescription(e.getMessage())
  - Map InvalidOrderException to Status.INVALID_ARGUMENT.withDescription(e.getMessage())
  - Map ConstraintViolationException to Status.INVALID_ARGUMENT with validation details
  - Map general Exception to Status.INTERNAL (log full stack trace)
  - Return asRuntimeException() for all statuses
  - _Requirements: 2.5_
  - _Leverage: OrderNotFoundException, InvalidOrderException from orders module_

### Phase 6: Health Check

- [x] 27. Create GrpcHealthIndicator class
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcHealthIndicator.java`
  - Add @Component annotation
  - Add @ConditionalOnClass(name = "io.grpc.Server")
  - Implement HealthIndicator interface
  - Inject io.grpc.Server in constructor
  - Create empty health() method structure
  - _Requirements: 2.6_
  - _Leverage: CacheHealthIndicator pattern (config.CacheHealthIndicator)_

- [x] 28. Implement health check logic in GrpcHealthIndicator
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcHealthIndicator.java` (continue from task 27)
  - Check if server is not null, not shutdown (!server.isShutdown()), not terminated (!server.isTerminated())
  - If healthy: return Health.up().withDetail("port", server.getPort()).withDetail("services", server.getServices().size())
  - If unhealthy: return Health.down().withDetail("reason", "gRPC server not running")
  - _Requirements: 2.6_
  - _Leverage: Spring Boot Actuator Health API_

### Phase 7: gRPC Client Implementation

- [x] 29. Create GrpcClientConfig configuration class
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcClientConfig.java`
  - Add @Configuration annotation
  - Add @ConditionalOnClass(name = "io.grpc.ManagedChannel")
  - Create empty class structure ready for client bean definitions
  - _Requirements: 3.1, 6.2_
  - _Leverage: CLAUDE.md conditional loading pattern_

- [x] 30. Implement ManagedChannel bean with connection pooling
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcClientConfig.java` (continue from task 29)
  - Create @Bean method for ManagedChannel
  - Inject GrpcProperties for client configuration
  - Use ManagedChannelBuilder.forTarget(properties.getClient().getTarget())
  - Add .usePlaintext() for development (note: use TLS in production)
  - Configure keepAlive settings for connection pooling
  - Return built channel
  - _Requirements: 3.1, 3.3_
  - _Leverage: gRPC ManagedChannelBuilder pattern_

- [x] 31. Create retry interceptor with exponential backoff
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcRetryInterceptor.java`
  - Implement ClientInterceptor interface
  - Inject GrpcProperties for retry configuration
  - Implement exponential backoff logic (base delay * 2^attempt)
  - Respect maxRetryAttempts from properties
  - Only retry on UNAVAILABLE and DEADLINE_EXCEEDED statuses
  - _Requirements: 3.2_
  - _Leverage: gRPC ClientInterceptor pattern_

- [x] 32. Add retry interceptor to ManagedChannel in GrpcClientConfig
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcClientConfig.java` (continue from task 30)
  - Create @Bean method for GrpcRetryInterceptor
  - Inject GrpcProperties
  - In ManagedChannel bean, add .intercept(retryInterceptor) if retryEnabled=true
  - Configure from properties.getClient().isRetryEnabled()
  - _Requirements: 3.2_
  - _Leverage: Spring conditional bean configuration_

- [x] 33. Create OrdersGrpcClient wrapper class
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcClient.java`
  - Add @Component annotation
  - Inject ManagedChannel and GrpcProperties
  - Create OrdersServiceBlockingStub field
  - Initialize stub in @PostConstruct method with channel
  - Set deadline from properties.getClient().getDeadlineMs()
  - Create empty class ready for wrapper methods
  - _Requirements: 3.1, 3.4_
  - _Leverage: gRPC stub pattern_

- [x] 34. Implement client wrapper methods in OrdersGrpcClient
  - File: `src/main/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcClient.java` (continue from task 33)
  - Implement createOrder(CreateOrderRequest) wrapper method
  - Implement getOrder(String orderNumber) wrapper method
  - Implement listOrders() wrapper method
  - Each method calls corresponding stub method with timeout
  - Handle StatusRuntimeException and convert to domain exceptions
  - _Requirements: 3.4, 3.5_
  - _Leverage: Generated OrdersServiceBlockingStub_

- [x] 35. Add client shutdown hook in GrpcClientConfig
  - File: `src/main/java/com/sivalabs/bookstore/config/GrpcClientConfig.java` (continue from task 32)
  - Add @PreDestroy method for graceful channel shutdown
  - Call channel.shutdown()
  - Wait for termination with timeout
  - Log shutdown completion
  - _Requirements: 3.3_
  - _Leverage: Spring lifecycle @PreDestroy pattern_

### Phase 8: Application Configuration

- [x] 36. Add gRPC server configuration to application.properties
  - File: `src/main/resources/application.properties`
  - Add bookstore.grpc.server.port=9091
  - Add bookstore.grpc.server.health-check-enabled=true
  - Add bookstore.grpc.server.reflection-enabled=true
  - Add bookstore.grpc.server.max-inbound-message-size=4194304
  - Add bookstore.grpc.server.shutdown-grace-period-seconds=30
  - _Requirements: 2.1, 6.1, 6.3_
  - _Leverage: Existing application.properties structure_

- [x] 37. Add gRPC client configuration to application.properties
  - File: `src/main/resources/application.properties` (continue from task 36)
  - Add bookstore.grpc.client.target=localhost:9091
  - Add bookstore.grpc.client.deadline-ms=5000
  - Add bookstore.grpc.client.retry-enabled=true
  - Add bookstore.grpc.client.max-retry-attempts=3
  - Add logging.level.io.grpc=INFO
  - Add logging.level.com.sivalabs.bookstore.orders.grpc=DEBUG
  - _Requirements: 3.1, 3.2, 3.4, 6.4_
  - _Leverage: Existing logging configuration pattern_

### Phase 9: Unit Testing

- [x] 38. Create GrpcMessageMapperTest unit test class
  - File: `src/test/java/com/sivalabs/bookstore/orders/grpc/GrpcMessageMapperTest.java`
  - Add @ExtendWith(MockitoExtension.class) annotation
  - Create test class with GrpcMessageMapper instance
  - Add @BeforeEach setup method creating mapper
  - Create test structure ready for mapping tests
  - _Requirements: 7.1_
  - _Leverage: Existing test infrastructure in orders module_

- [x] 39. Write Protobuf to DTO conversion tests
  - File: `src/test/java/com/sivalabs/bookstore/orders/grpc/GrpcMessageMapperTest.java` (continue from task 38)
  - Test toCreateOrderRequest() with valid Protobuf message
  - Verify Customer fields map correctly (name, email, phone)
  - Verify OrderItem fields including BigDecimal price conversion from string
  - Test null handling for optional fields
  - Use AssertJ assertions (assertThat)
  - _Requirements: 5.1, 5.3, 5.5, 7.1, 7.3_
  - _Leverage: AssertJ assertion library_

- [x] 40. Write DTO to Protobuf conversion tests
  - File: `src/test/java/com/sivalabs/bookstore/orders/grpc/GrpcMessageMapperTest.java` (continue from task 39)
  - Test toCreateOrderResponse() conversion
  - Test toOrderDto() with all fields including timestamp and total amount
  - Test toOrderView() conversion
  - Verify BigDecimal to String preserves precision (no rounding)
  - Verify LocalDateTime to Timestamp conversion with timezone
  - _Requirements: 5.2, 5.4, 5.5, 7.1, 7.3_
  - _Leverage: Existing DTO test data patterns_

- [x] 41. Create GrpcExceptionHandlerTest unit test class
  - File: `src/test/java/com/sivalabs/bookstore/orders/grpc/GrpcExceptionHandlerTest.java`
  - Test OrderNotFoundException maps to Status.NOT_FOUND
  - Test InvalidOrderException maps to Status.INVALID_ARGUMENT
  - Test ConstraintViolationException maps to Status.INVALID_ARGUMENT
  - Test generic Exception maps to Status.INTERNAL
  - Verify status descriptions are set from exception messages
  - Use assertThat for StatusRuntimeException assertions
  - _Requirements: 2.5, 7.1, 7.3_
  - _Leverage: Existing OrderNotFoundException, InvalidOrderException_

### Phase 10: Integration Testing

- [x] 42. Create OrdersGrpcServiceIntegrationTest class structure
  - File: `src/test/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcServiceIntegrationTest.java`
  - Add @SpringBootTest annotation
  - Add @ApplicationModuleTest annotation for orders module isolation
  - Add @Testcontainers annotation
  - Add @Container PostgreSQLContainer for database
  - Inject OrdersGrpcService
  - Create in-process gRPC server setup in @BeforeEach
  - Create channel and stub initialization
  - Create @AfterEach for cleanup
  - _Requirements: 7.2, 7.5_
  - _Leverage: Existing integration test patterns (CatalogIntegrationTests, OrdersIntegrationTests)_

- [x] 43. Write createOrder integration test
  - File: `src/test/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcServiceIntegrationTest.java` (continue from task 42)
  - Create ManagedChannel with InProcessChannelBuilder.forName("test-server")
  - Create OrdersServiceBlockingStub from channel
  - Build valid CreateOrderRequest Protobuf message
  - Call stub.createOrder(request)
  - Verify response has non-empty order number
  - Query database to verify order is persisted
  - Verify order fields match request
  - Clean up channel in finally block
  - _Requirements: 2.4, 7.2, 7.3, 7.5_
  - _Leverage: gRPC in-process testing utilities (InProcessServerBuilder)_

- [x] 44. Write getOrder integration tests for success and not found
  - File: `src/test/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcServiceIntegrationTest.java` (continue from task 43)
  - Test 1: Create order then getOrder with that orderNumber returns order
  - Verify all fields are populated correctly
  - Test 2: Call getOrder with non-existent orderNumber
  - Verify StatusRuntimeException thrown with Status.NOT_FOUND
  - Verify error description contains order number
  - _Requirements: 2.4, 2.5, 7.2, 7.3_
  - _Leverage: Existing test data setup from OrdersIntegrationTests_

- [x] 45. Write listOrders integration test
  - File: `src/test/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcServiceIntegrationTest.java` (continue from task 44)
  - Create 3 orders via createOrder
  - Call listOrders() RPC
  - Verify response contains all 3 orders
  - Verify order numbers match created orders
  - Verify data integrity across gRPC boundary
  - _Requirements: 2.4, 7.2, 7.3_
  - _Leverage: Existing list test patterns_

- [x] 46. Write error scenario integration tests
  - File: `src/test/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcServiceIntegrationTest.java` (continue from task 45)
  - Test createOrder with empty customer name throws INVALID_ARGUMENT
  - Test createOrder with negative quantity throws INVALID_ARGUMENT
  - Test createOrder with invalid product code throws FAILED_PRECONDITION or NOT_FOUND
  - Verify proper gRPC status codes returned
  - Verify error descriptions are meaningful and contain field names
  - _Requirements: 2.5, 7.3_
  - _Leverage: Existing validation and ProductServiceClient error handling_

- [x] 47. Create GrpcClientIntegrationTest for client wrapper
  - File: `src/test/java/com/sivalabs/bookstore/orders/grpc/OrdersGrpcClientIntegrationTest.java`
  - Add @SpringBootTest annotation
  - Add @Testcontainers
  - Start in-process gRPC server with OrdersGrpcService
  - Inject OrdersGrpcClient
  - Test client.createOrder() method
  - Test client.getOrder() method
  - Test client.listOrders() method
  - Verify client properly propagates exceptions
  - _Requirements: 3.5, 7.2, 7.3_
  - _Leverage: In-process server pattern from task 42_

### Phase 11: REST Endpoint Removal & Documentation

- [x] 48. Update monolith OrdersWebController to use gRPC client
  - File: `src/main/java/com/sivalabs/bookstore/web/OrdersWebController.java`
  - Remove `RestTemplate` and `@Value("${orders.service.api-url}")` dependencies
  - Inject `OrdersGrpcClient` (from `src/main/java/com/sivalabs/bookstore/orders/grpc/`)
  - Update `createOrder()` method: change to use `grpcClient.createOrder(request)`
  - Update `showOrders()` method: change to use `grpcClient.listOrders()`
  - Update `showOrderDetails()` method: change to use `grpcClient.getOrder(orderNumber)`
  - Handle gRPC exception conversion to HTTP responses:
    - `StatusRuntimeException` with `Status.NOT_FOUND` → return 404 error page
    - `StatusRuntimeException` with `Status.INVALID_ARGUMENT` → return 400 error page with validation message
    - Other exceptions → return 500 error page with generic error message
  - Test the changes by running the application and verifying order creation flow works via UI
  - _Requirements: 4.1, 4.2_
  - _Leverage: OrdersGrpcClient (Tasks 33-34), GrpcExceptionHandler pattern_

- [x] 49. Remove or disable orders service REST API endpoint
  - File: `orders/src/main/java/com/sivalabs/bookstore/orders/web/OrderRestController.java`
  - **Recommended approach**: Add `@ConditionalOnProperty(name = "orders.rest.enabled", havingValue = "true", matchIfMissing = false)` to class
  - **Alternative approach**: Delete the file completely (more aggressive, harder to rollback)
  - Add JavaDoc deprecation notice: `@deprecated Migrated to gRPC. See OrdersGrpcService for gRPC implementation.`
  - Add comment explaining migration date and reason
  - Keep the code for emergency rollback capability
  - _Requirements: 4.2_
  - _Leverage: Spring conditional loading pattern (see GrpcServerConfig @ConditionalOnClass)_

- [x] 50. Clean up REST API related configuration
  - Files:
    - `src/main/resources/application.properties`
    - `compose.yml`
  - **application.properties**: Remove or comment out `orders.service.api-url=${ORDERS_SERVICE_API_URL:http://localhost:8091/api/orders}`
  - **compose.yml monolith service**: Remove environment variable `ORDERS_SERVICE_API_URL: http://orders-service:8091/api/orders`
  - **compose.yml orders-service**:
    - Remove or comment out `SERVER_PORT: 8091` (if completely disabling REST)
    - Keep gRPC port mapping: `9090:9090`
  - **compose.yml monolith service**: Verify `BOOKSTORE_GRPC_CLIENT_TARGET: orders-service:9090` is present and correct
  - Add comments explaining configuration changes with date and migration reference
  - _Requirements: 4.1, 4.3_
  - _Leverage: Existing compose.yml and application.properties structure_

- [x] 51. Verify REST endpoint completely removed and gRPC working
  - Start services: `docker-compose up -d`
  - **Verify REST is disabled**:
    - Run: `curl http://localhost:8091/api/orders`
    - Expected: Connection refused or 404 Not Found
  - **Verify gRPC is available**:
    - Run: `grpcurl -plaintext localhost:9090 list`
    - Expected: Shows `com.sivalabs.bookstore.orders.grpc.proto.OrdersService`
  - **End-to-end UI test**:
    - Open browser: `http://localhost:8080/cart`
    - Add a product to cart
    - Fill in customer details and create order
    - Verify order details page displays correctly
    - Verify order appears in orders list
  - **Check logs for gRPC communication**:
    - Should see `[ORDERS-CLIENT]` logs from monolith service
    - Should see `[ORDERS-SERVER]` logs from orders-service
    - Should NOT see any REST API related errors
  - Stop services: `docker-compose down`
  - _Requirements: 4.1, 4.2, 4.4_
  - _Leverage: Existing testing workflows and grpcurl tool_

- [x] 52. Update CLAUDE.md with gRPC endpoints documentation
  - File: `CLAUDE.md`
  - Add gRPC Server entry under Application URLs section: `localhost:9090` (orders-service gRPC)
  - Update Module Communication Patterns section:
    - Note that Orders module communication now uses gRPC instead of REST
    - Explain monolith → orders-service communication via gRPC
  - Add testing guidance with grpcurl command examples
  - Document that health check at `/actuator/health` includes gRPC status
  - Add note about server reflection enabled for development
  - **Add migration note**: Document that REST API (`http://localhost:8091/api/orders`) has been removed, only gRPC is available
  - _Requirements: 4.1, 4.3_
  - _Leverage: Existing CLAUDE.md structure and format_

### Phase 12: Final Validation

- [x] 53. Run complete test suite and verify all tests pass
  - Run: `./mvnw clean verify`
  - Verify all unit tests pass (GrpcMessageMapperTest, GrpcExceptionHandlerTest)
  - Verify all integration tests pass (OrdersGrpcServiceIntegrationTest, GrpcClientIntegrationTest)
  - Verify no test failures or compilation errors
  - Check test coverage meets project standards (≥80% unit, ≥70% integration per requirements)
  - _Requirements: All, NFR: Reliability_
  - _Leverage: Existing Maven test execution and reporting_

- [x] 54. Verify application startup with gRPC server
  - Run: `./mvnw spring-boot:run`
  - Verify application starts without errors
  - Verify log shows "gRPC server started on port 9091" message
  - Verify log shows OrdersService registered
  - Verify no "ConditionalOnClass" errors or bean creation failures
  - Stop application and verify graceful shutdown logs
  - _Requirements: 2.1, 2.2, 6.2_
  - _Leverage: Existing application startup validation_

- [ ] 55. Verify Spring Actuator health endpoint shows gRPC status
  - Start application with `./mvnw spring-boot:run`
  - Access http://localhost:8080/actuator/health in browser or curl
  - Verify JSON response includes "grpc" component
  - Verify grpc status shows "UP"
  - Verify details include port (9091) and service count
  - Stop application and verify health shows DOWN
  - _Requirements: 2.6, NFR: Observability_
  - _Leverage: Existing Spring Actuator health check infrastructure_

- [ ] 56. Manual functional testing with grpcurl
  - Install grpcurl if not available: `brew install grpcurl` (macOS) or download binary
  - Start application
  - Test server reflection: `grpcurl -plaintext localhost:9091 list`
  - Verify OrdersService is listed
  - Test createOrder: `grpcurl -plaintext -d '{"customer":{"name":"Test","email":"test@example.com","phone":"123"},"deliveryAddress":"Test Address","item":{"code":"P001","name":"Book","price":"19.99","quantity":1}}' localhost:9091 com.sivalabs.bookstore.orders.grpc.proto.OrdersService/CreateOrder`
  - Copy order number from response
  - Test getOrder: `grpcurl -plaintext -d '{"order_number":"ORDER-XXX"}' localhost:9091 com.sivalabs.bookstore.orders.grpc.proto.OrdersService/GetOrder`
  - Test listOrders: `grpcurl -plaintext -d '{}' localhost:9091 com.sivalabs.bookstore.orders.grpc.proto.OrdersService/ListOrders`
  - Verify all operations return expected results
  - _Requirements: 2.7, 7.4, NFR: Usability_
  - _Leverage: gRPC server reflection feature_

- [ ] 57. Performance comparison with previous REST baseline
  - Create simple JMH or JUnit benchmark test
  - Measure gRPC createOrder latency (average over 100 requests)
  - Measure message size: Protobuf CreateOrderRequest vs equivalent JSON
  - Verify gRPC latency meets <50ms requirement for local communication
  - Verify Protobuf message is smaller than JSON (expect 30%+ reduction)
  - Document findings in test output or separate performance report
  - _Requirements: 7.4, NFR: Performance_
  - _Leverage: JUnit 5 benchmark extensions or simple timing in test_

- [ ] 58. Final integration verification and migration report
  - Run complete end-to-end flow test (UI → gRPC → Database → Events)
  - Verify no REST endpoints remain accessible on port 8091
  - Confirm all documentation updates are complete (CLAUDE.md, README files)
  - Verify Docker Compose setup works correctly with gRPC-only configuration
  - Generate migration completion report with:
    - Performance comparison results (REST vs gRPC)
    - Test coverage metrics
    - List of removed/deprecated components
    - Rollback procedure documentation
  - _Requirements: All, NFR: Maintainability_
  - _Leverage: All previous validation tasks_

## Implementation Notes

### Build and Test Commands
```bash
# Compile protobuf files
./mvnw clean compile

# Run all tests
./mvnw clean verify

# Run specific test class
./mvnw test -Dtest=OrdersGrpcServiceIntegrationTest

# Run specific test method
./mvnw test -Dtest=OrdersGrpcServiceIntegrationTest#shouldCreateOrderViaGrpc

# Format code
./mvnw spotless:apply

# Run application
./mvnw spring-boot:run
```

### Testing with grpcurl
```bash
# List all services
grpcurl -plaintext localhost:9091 list

# List methods for OrdersService
grpcurl -plaintext localhost:9091 list com.sivalabs.bookstore.orders.grpc.proto.OrdersService

# Describe OrdersService
grpcurl -plaintext localhost:9091 describe com.sivalabs.bookstore.orders.grpc.proto.OrdersService

# Describe CreateOrderRequest message
grpcurl -plaintext localhost:9091 describe com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest
```

### Task Dependencies and Ordering

**Critical Path:**
1. Tasks 1-7 must complete before any code implementation (protobuf compilation required)
2. Tasks 8-14 (configuration) must complete before tasks 21-24 (service implementation)
3. Tasks 15-20 (mapper) must complete before tasks 21-24 (service using mapper)
4. Tasks 21-24 (service) must complete before tasks 29-35 (client needs server stubs)
5. Tasks 38-47 (testing) depend on all implementation tasks
6. **Tasks 48-52 (REST removal/docs) must execute in order**:
   - Task 48 (monolith uses gRPC) must complete BEFORE task 49
   - Task 49 (disable REST API) must complete BEFORE task 50
   - Task 50 (clean config) must complete BEFORE task 51
   - Task 51 (verify removal) must complete BEFORE task 52
7. Tasks 53-58 (final validation) must be last

**Parallel Opportunities:**
- Tasks 8-14 (config) and 15-20 (mapper) can be done in parallel after task 7
- Tasks 25-26 (exception handler) and 27-28 (health) can be done in parallel with tasks 15-24
- Tasks 38-41 (unit tests) can be written in parallel with integration test setup (task 42)
- Tasks 43-46 (integration tests) can be written in parallel
- Tasks 36-37 (properties) can be done early or late, but should be before task 54
- Tasks 53-57 (validation tests) can potentially run in parallel if properly isolated

### Critical Success Factors

- All Protocol Buffer messages must compile without errors (task 7 checkpoint)
- GrpcServerConfig must use conditional loading to prevent startup failures when gRPC jars missing
- Message mapping must preserve BigDecimal precision (no rounding during string conversion)
- Timestamp conversion must handle timezones correctly (use system default or UTC consistently)
- Exception handling must map all domain exceptions to appropriate gRPC status codes
- Client retry logic must respect maxRetryAttempts and only retry on retryable statuses
- **Task 48 must correctly handle gRPC exceptions and convert to appropriate HTTP error responses**
- **Task 49 should use @ConditionalOnProperty for safety, not delete files immediately**
- **Task 51 end-to-end test must pass before considering REST removal complete**
- All tests must pass before considering migration complete (task 53 gate)
- Task 58 migration report must document rollback procedure
