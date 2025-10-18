# Requirements Document

## Introduction

This specification defines the requirements for adding comprehensive OpenAPI/REST API support to the BookStore Modular Monolith application. The feature will enable frontend applications (SPAs, mobile apps) to interact with the system through standardized REST endpoints while maintaining compatibility with the existing server-rendered web interface. This implementation will provide interactive API documentation, auto-generated client SDKs, and a clear migration path toward frontend-backend separation.

## Alignment with Product Vision

This feature aligns with the product vision in the following ways:

- **Educational Demonstration**: Showcases modern API documentation practices and frontend-backend separation patterns using Spring Boot and OpenAPI standards
- **Production Readiness**: Provides enterprise-grade API documentation, error handling, and client SDK generation capabilities
- **Future Roadmap Enablement**: Supports the medium-term goal of "GraphQL API layer for improved client flexibility" by first establishing a robust REST API foundation

This enhancement supports the architectural goal of maintaining flexibility while preserving the educational value of demonstrating both traditional server-rendered and modern API-driven architectures within a single modular monolith.

## Requirements

### Requirement 1: Products REST API with OpenAPI Documentation

**User Story:** As a frontend developer, I want to access product catalog data through a documented REST API, so that I can build SPA applications that display products and handle search/filtering

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/products?page=N` THEN the system SHALL return a paginated JSON response containing product list with metadata (total pages, current page, has next/previous)
2. WHEN a GET request is made to `/api/products/{code}` THEN the system SHALL return a single product JSON object or 404 if not found
3. WHEN accessing `/swagger-ui.html` THEN the system SHALL display interactive API documentation for all product endpoints with request/response examples
4. WHEN OpenAPI spec is exported from `/api-docs` THEN the system SHALL provide a valid OpenAPI 3.0 JSON document that can generate client SDKs

### Requirement 2: Shopping Cart REST API

**User Story:** As a frontend developer, I want to manage shopping cart operations through REST API, so that I can build stateful shopping experiences in SPA applications

#### Acceptance Criteria

1. WHEN a POST request is made to `/api/cart/items` with product code and quantity THEN the system SHALL add the item to the user's cart and return the updated cart state
2. WHEN a PUT request is made to `/api/cart/items/{code}` with updated quantity THEN the system SHALL update the item quantity and return the updated cart
3. WHEN a GET request is made to `/api/cart` THEN the system SHALL return the current cart contents with calculated total amount
4. WHEN a DELETE request is made to `/api/cart` THEN the system SHALL clear the cart and return empty cart state
5. IF cart operations fail due to invalid product code THEN the system SHALL return HTTP 404 with descriptive error message

### Requirement 3: Orders REST API with gRPC Integration

**User Story:** As a frontend developer, I want to create and query orders through REST API, so that I can build complete checkout flows in frontend applications

#### Acceptance Criteria

1. WHEN a POST request is made to `/api/orders` with valid order data THEN the system SHALL create an order via gRPC client and return HTTP 201 with order number
2. WHEN a GET request is made to `/api/orders` THEN the system SHALL retrieve all orders via gRPC client and return JSON array of order summaries
3. WHEN a GET request is made to `/api/orders/{orderNumber}` THEN the system SHALL retrieve order details via gRPC client and return complete order JSON
4. IF order creation fails due to gRPC errors THEN the system SHALL map gRPC status codes to appropriate HTTP status codes (NOT_FOUND→404, INVALID_ARGUMENT→400, etc.)
5. WHEN gRPC communication fails THEN the system SHALL return HTTP 503 with Service Unavailable error message

### Requirement 4: Comprehensive Error Handling and Validation

**User Story:** As a frontend developer, I want consistent error responses from all API endpoints, so that I can handle errors uniformly in my application

#### Acceptance Criteria

1. WHEN any API endpoint encounters an error THEN the system SHALL return a JSON error response with fields: `status`, `message`, and `timestamp`
2. WHEN validation fails on request bodies THEN the system SHALL return HTTP 400 with field-level validation error details
3. WHEN a resource is not found THEN the system SHALL return HTTP 404 with descriptive message indicating which resource was not found
4. WHEN an internal error occurs THEN the system SHALL return HTTP 500 with a generic error message (not exposing internal details)
5. IF gRPC backend is unavailable THEN the system SHALL return HTTP 503 with circuit breaker status information

### Requirement 5: API Documentation and Client SDK Generation

**User Story:** As a developer integrating with the bookstore API, I want comprehensive API documentation and auto-generated client libraries, so that I can quickly understand and integrate with the system

#### Acceptance Criteria

1. WHEN accessing Swagger UI at `/swagger-ui.html` THEN the system SHALL display all REST endpoints organized by tags (Products, Cart, Orders)
2. WHEN viewing any endpoint documentation THEN the system SHALL show request parameters, request body schemas, response schemas, and error responses
3. WHEN the OpenAPI specification is retrieved from `/api-docs` THEN the system SHALL provide a complete OpenAPI 3.0 document with all DTOs documented using `@Schema` annotations
4. WHEN using OpenAPI Generator with the exported spec THEN the system SHALL successfully generate TypeScript/JavaScript client SDKs without errors
5. IF API structure changes THEN the system SHALL automatically update the OpenAPI spec to reflect the changes

## Non-Functional Requirements

### Code Architecture and Modularity

- **Single Responsibility Principle**: REST controllers shall only handle HTTP concerns; business logic remains in existing gRPC clients and service layers
- **Modular Design**: REST API layer shall be added without modifying existing Web Controllers or gRPC implementations
- **Dependency Management**: REST controllers shall reuse existing `OrdersRemoteClient`, `ProductApi`, and cart utilities; no duplicate business logic
- **Clear Interfaces**: REST endpoints shall use the same DTOs as Web Controllers where possible; new DTOs shall follow existing naming conventions (e.g., `ProductDto`, `CartDto`)

### Performance

- **Response Time**: REST API endpoints shall respond within same latency targets as existing system (<200ms for orders, <100ms for catalog queries)
- **No Additional Overhead**: REST layer shall add <10ms overhead compared to direct gRPC calls
- **Session Management**: Cart operations shall reuse existing Hazelcast session store; no performance degradation from dual access patterns
- **Caching**: Product API responses shall leverage existing ProductCacheService; no cache duplication

### Security

- **Input Validation**: All REST endpoints shall validate request bodies using Spring Validation (`@Valid`, `@NotNull`, `@NotBlank`)
- **Error Information**: Error responses shall not expose stack traces, internal paths, or sensitive system information
- **Session Security**: Cart and order operations shall maintain existing session-based security model
- **CORS Configuration**: API shall support configurable CORS settings for frontend applications (default: same-origin)

### Reliability

- **gRPC Fault Tolerance**: When gRPC calls fail, REST API shall return appropriate HTTP error codes with retry guidance
- **Graceful Degradation**: If OpenAPI documentation generation fails, API endpoints shall continue to function
- **Circuit Breaker**: REST layer shall respect existing circuit breaker patterns from gRPC client layer
- **Transaction Consistency**: Order creation via REST shall maintain same transactional guarantees as Web Controller implementation

### Usability

- **API Discoverability**: Swagger UI shall be accessible from application home page with clear documentation
- **Consistent Naming**: REST endpoint paths shall follow RESTful conventions (`/api/{resource}`, `/api/{resource}/{id}`)
- **HTTP Semantics**: Endpoints shall use appropriate HTTP methods (GET for queries, POST for creation, PUT for updates, DELETE for removal)
- **Developer Experience**: OpenAPI spec shall include meaningful descriptions, examples, and error scenarios for all endpoints

### Compatibility

- **Backward Compatibility**: Existing Web Controllers and Thymeleaf templates shall continue to function unchanged
- **Coexistence**: REST API and Web Controllers shall share same session management and business logic without conflicts
- **Framework Compatibility**: OpenAPI integration shall be compatible with Spring Boot 3.5.5 and Spring Modulith 1.4.3
- **Client Support**: Generated SDKs shall support modern JavaScript/TypeScript environments (ES2020+, TypeScript 4.5+)
