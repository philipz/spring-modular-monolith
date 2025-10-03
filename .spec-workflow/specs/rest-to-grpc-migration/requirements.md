# Requirements Document: REST to gRPC Complete Migration for Orders Module

## Introduction

This specification defines the complete replacement of REST API with gRPC protocol for the Orders module in the Spring Modular Monolith bookstore application. The migration aims to improve performance, type safety, and cross-language compatibility by using only gRPC for inter-module communication.

The current architecture uses REST APIs for module-to-module communication (e.g., Orders → Catalog for product validation). This migration will completely replace REST communication with gRPC protocol, leveraging Protocol Buffers for efficient serialization and strongly-typed service contracts.

## Alignment with Product Vision

This migration supports the following architectural goals:

- **Performance**: gRPC provides faster serialization/deserialization compared to JSON REST APIs, reducing latency for inter-module calls
- **Type Safety**: Protocol Buffers provide compile-time type checking, reducing runtime errors
- **Service Evolution**: gRPC's built-in versioning and backward compatibility features enable safer API evolution
- **Modularity**: Maintains Spring Modulith principles while using gRPC as the primary communication mechanism
- **Observability**: Integrates with existing tracing infrastructure (OpenTelemetry/Zipkin)

## Requirements

### Requirement 1: gRPC Service Definition

**User Story:** As a developer, I want to define Orders service contracts using Protocol Buffers, so that I have strongly-typed, language-agnostic service definitions.

#### Acceptance Criteria

1. WHEN the project is built THEN Protocol Buffer files SHALL be compiled into Java classes
2. WHEN defining service methods THEN they SHALL mirror existing REST API operations (createOrder, findOrder, findOrders)
3. IF a message type is defined THEN it SHALL include all fields from corresponding REST DTOs
4. WHEN designing the proto schema THEN it SHALL follow Google's protobuf style guide
5. WHEN versioning is needed THEN the proto package SHALL support backward-compatible changes

### Requirement 2: gRPC Server Implementation

**User Story:** As a system administrator, I want the monolith application to expose only gRPC endpoints, so that we have a single, efficient communication protocol.

#### Acceptance Criteria

1. WHEN the application starts THEN a gRPC server SHALL start on port 9091
2. IF gRPC dependencies are not available THEN the application SHALL fail to start with a clear error message
3. WHEN a gRPC call is received THEN it SHALL be handled by the OrdersGrpcService implementation
4. WHEN processing gRPC requests THEN they SHALL use the existing OrdersApi service layer
5. IF an error occurs THEN it SHALL be mapped to appropriate gRPC status codes
6. WHEN the application is running THEN gRPC health checks SHALL be available
7. WHEN in development mode THEN gRPC server reflection SHALL be enabled

### Requirement 3: gRPC Client Implementation

**User Story:** As a microservice developer, I want to consume Orders gRPC services from external services, so that I can integrate with the monolith using efficient protocols.

#### Acceptance Criteria

1. WHEN a client is configured THEN it SHALL connect to the gRPC server on localhost:9091
2. IF connection fails THEN the client SHALL retry with exponential backoff
3. WHEN multiple requests are made THEN connections SHALL be reused (channel pooling)
4. IF a call timeout occurs THEN it SHALL fail after configurable deadline
5. WHEN errors occur THEN they SHALL be properly propagated with context
6. WHEN tracing is enabled THEN gRPC calls SHALL include trace context propagation

### Requirement 4: REST Endpoint Removal

**User Story:** As a system architect, I want to remove REST endpoints for Orders API, so that we have a single gRPC-based communication protocol.

#### Acceptance Criteria

1. WHEN the application runs THEN only gRPC (port 9091) endpoints SHALL be available for Orders operations
2. IF REST controllers exist for Orders API THEN they SHALL be removed or disabled
3. WHEN a gRPC request arrives THEN the same business logic SHALL be executed as previous REST implementation
4. IF data validation fails THEN gRPC SHALL return appropriate status codes and error details
5. WHEN monitoring the system THEN metrics SHALL be collected only for gRPC calls

### Requirement 5: Data Mapping and Conversion

**User Story:** As a developer, I want automatic conversion between Protobuf messages and Java DTOs, so that I can reuse existing business logic without duplication.

#### Acceptance Criteria

1. WHEN a gRPC request is received THEN it SHALL be converted to CreateOrderRequest DTO
2. WHEN a service returns data THEN DTOs SHALL be converted to Protobuf response messages
3. IF optional fields are missing THEN conversions SHALL handle null values safely
4. WHEN timestamps are involved THEN they SHALL be converted to/from google.protobuf.Timestamp
5. WHEN money values are used THEN they SHALL preserve precision during conversion

### Requirement 6: gRPC Configuration

**User Story:** As a system administrator, I want to configure gRPC server and client settings through application properties, so that I can customize behavior without code changes.

#### Acceptance Criteria

1. WHEN the application starts THEN gRPC server SHALL start by default
2. IF gRPC dependencies are missing THEN application SHALL fail to start with clear error message
3. WHEN configuring gRPC THEN server port SHALL be customizable via `bookstore.grpc.server.port` (default: 9091)
4. WHEN client configuration is needed THEN connection details SHALL be externalized to properties
5. WHEN security is enabled THEN TLS configuration SHALL be supported

### Requirement 7: Testing and Validation

**User Story:** As a QA engineer, I want comprehensive tests for gRPC services, so that I can verify functionality and prevent regressions.

#### Acceptance Criteria

1. WHEN gRPC services are tested THEN unit tests SHALL verify message mapping
2. IF integration testing is performed THEN in-process gRPC server SHALL be used
3. WHEN testing client-server interaction THEN both success and error scenarios SHALL be covered
4. IF performance is measured THEN gRPC latency SHALL be compared with previous REST baseline
5. WHEN running tests THEN they SHALL not depend on external gRPC servers

## Non-Functional Requirements

### Performance

- gRPC calls SHALL complete with <50ms latency for local communication
- Message serialization SHALL be at least 30% faster than previous JSON REST implementation
- Server SHALL support at least 1000 concurrent gRPC streams
- Memory overhead SHALL not exceed 50MB for gRPC infrastructure

### Security

- gRPC server SHALL support TLS encryption for production deployments
- Authentication tokens SHALL be transmitted via metadata
- Authorization SHALL be enforced before processing gRPC requests
- Service methods SHALL validate all input parameters

### Reliability

- gRPC client SHALL implement retry logic with exponential backoff
- Circuit breaker SHALL open after 5 consecutive failures
- Health checks SHALL return service status accurately
- Server SHALL gracefully shutdown without dropping active requests

### Usability

- Proto files SHALL include comprehensive documentation comments
- Error messages SHALL be clear and actionable
- Configuration properties SHALL have sensible defaults
- Server reflection SHALL enable CLI tools (grpcurl) for testing

### Compatibility

- Protocol Buffers version 3 (proto3) SHALL be used
- Generated code SHALL be compatible with Java 21
- gRPC version SHALL align with Spring Boot's managed dependencies
- Changes SHALL maintain Spring Modulith module boundaries

### Observability

- gRPC calls SHALL integrate with OpenTelemetry tracing
- Metrics SHALL be exported to Prometheus
- Server and client interceptors SHALL log requests/responses
- Health indicator SHALL report gRPC server status in Spring Actuator
