# Tasks Document

## Task 1: Add OpenAPI Dependencies and Configuration

- [x] 1. Add springdoc-openapi dependency to pom.xml
  - Files: `pom.xml`
  - Add springdoc-openapi-starter-webmvc-ui dependency (version 2.6.0)
  - Purpose: Enable OpenAPI 3.0 documentation generation and Swagger UI
  - _Leverage: Existing Spring Boot 3.5.5 dependency management_
  - _Requirements: Requirement 5 (API Documentation)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Build Engineer with Maven expertise | Task: Add springdoc-openapi-starter-webmvc-ui dependency (version 2.6.0) to pom.xml following Requirement 5, ensuring compatibility with Spring Boot 3.5.5 | Restrictions: Do not modify existing dependencies, maintain Maven Wrapper compatibility, follow project's dependency management patterns | _Leverage: Existing Spring Boot parent POM dependency management | Success: Dependency added successfully, Maven build succeeds, no version conflicts | Instructions: First update tasks.md to mark this task as in-progress [-], then add the dependency and verify build, finally mark as complete [x] in tasks.md_

- [x] 2. Configure OpenAPI in application.properties
  - Files: `src/main/resources/application.properties`
  - Add springdoc configuration properties (api-docs path, swagger-ui path, grouping)
  - Purpose: Configure OpenAPI documentation endpoints and behavior
  - _Leverage: Existing application properties structure_
  - _Requirements: Requirement 5 (API Documentation)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring Boot Configuration Specialist | Task: Add springdoc OpenAPI configuration properties to application.properties following Requirement 5, including api-docs path (/api-docs), swagger-ui path (/swagger-ui.html), and API grouping configuration | Restrictions: Do not modify existing properties, follow property naming conventions, maintain configuration organization | _Leverage: Existing property organization patterns in application.properties | Success: Properties added correctly, Swagger UI accessible at /swagger-ui.html, OpenAPI spec available at /api-docs | Instructions: First mark task as in-progress [-] in tasks.md, add properties, verify endpoints work, then mark complete [x]_

- [x] 3. Create OpenApiConfig configuration class
  - Files: `src/main/java/com/sivalabs/bookstore/config/OpenApiConfig.java`
  - Create @Configuration class with @Bean method returning OpenAPI object
  - Define API info (title, version, description, servers)
  - Purpose: Provide programmatic OpenAPI customization
  - _Leverage: Existing config package structure_
  - _Requirements: Requirement 5 (API Documentation)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring Boot Configuration Developer | Task: Create OpenApiConfig class in src/main/java/com/sivalabs/bookstore/config/ following Requirement 5, defining API metadata (title: "BookStore REST API", version: "1.0.0", description, server URL) | Restrictions: Follow existing configuration class patterns, use Spring Boot conventions, do not hardcode URLs (use properties if needed) | _Leverage: Existing configuration classes in config package | Success: Configuration class compiles, OpenAPI metadata appears in Swagger UI, follows project coding standards | Instructions: Mark task in-progress [-], create class, verify Swagger UI shows metadata, mark complete [x]_

## Task 2: Enhance Products REST API with OpenAPI Annotations

- [x] 4. Add OpenAPI annotations to ProductRestController
  - Files: `src/main/java/com/sivalabs/bookstore/catalog/web/ProductRestController.java`
  - Add @Tag, @Operation, @ApiResponse annotations to controller and methods
  - Purpose: Document Products API endpoints in OpenAPI spec
  - _Leverage: Existing ProductRestController implementation_
  - _Requirements: Requirement 1 (Products REST API)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: API Documentation Specialist with Spring Boot expertise | Task: Add comprehensive OpenAPI annotations (@Tag, @Operation, @ApiResponses) to ProductRestController following Requirement 1, documenting all endpoints with descriptions, parameters, and response codes | Restrictions: Do not modify existing logic, only add annotations, ensure all HTTP status codes are documented (200, 400, 404) | _Leverage: Existing controller methods and logic in ProductRestController | Success: All endpoints show in Swagger UI with descriptions, examples, and error responses | Instructions: Mark in-progress [-], add annotations, verify in Swagger UI, mark complete [x]_

- [x] 5. Add @Schema annotations to ProductDto
  - Files: `src/main/java/com/sivalabs/bookstore/catalog/api/ProductDto.java`
  - Add @Schema annotations to all fields with descriptions and examples
  - Purpose: Document Product data structure in OpenAPI spec
  - _Leverage: Existing ProductDto record definition_
  - _Requirements: Requirement 1 (Products REST API)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Developer with OpenAPI schema expertise | Task: Add @Schema annotations to all ProductDto fields following Requirement 1, including field descriptions (code, name, price, imageUrl) and example values | Restrictions: Do not modify record structure, only add annotations, ensure examples are realistic | _Leverage: Existing ProductDto record in catalog.api package | Success: ProductDto schema shows in OpenAPI spec with all fields documented and examples | Instructions: Mark in-progress [-], add @Schema to fields, verify schema in Swagger, mark complete [x]_

- [x] 6. Add @Schema annotations to PagedResult
  - Files: `src/main/java/com/sivalabs/bookstore/common/models/PagedResult.java`
  - Add @Schema annotations for pagination metadata fields
  - Purpose: Document pagination structure in OpenAPI spec
  - _Leverage: Existing PagedResult generic class_
  - _Requirements: Requirement 1 (Products REST API)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Developer with generics and OpenAPI expertise | Task: Add @Schema annotations to PagedResult fields following Requirement 1, documenting pagination metadata (data, pageNumber, totalPages, hasNext, hasPrevious, etc.) | Restrictions: Maintain generic type compatibility, do not change class structure, ensure generic type T is properly documented | _Leverage: Existing PagedResult class in common.models package | Success: PagedResult schema appears in OpenAPI with all pagination fields documented | Instructions: Mark in-progress [-], add annotations, verify in Swagger, mark complete [x]_

## Task 3: Create Cart REST API

- [x] 7. Create CartDto and related DTOs
  - Files:
    - `src/main/java/com/sivalabs/bookstore/web/dto/CartDto.java`
    - `src/main/java/com/sivalabs/bookstore/web/dto/CartItemDto.java`
    - `src/main/java/com/sivalabs/bookstore/web/dto/AddToCartRequest.java`
    - `src/main/java/com/sivalabs/bookstore/web/dto/UpdateQuantityRequest.java`
  - Create record DTOs with @Schema annotations and validation annotations
  - Purpose: Define Cart API data structures
  - _Leverage: Existing Cart and Cart.LineItem models in web package_
  - _Requirements: Requirement 2 (Shopping Cart REST API)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java DTO Design Specialist | Task: Create Cart-related DTOs (CartDto, CartItemDto, AddToCartRequest, UpdateQuantityRequest) following Requirement 2 and design.md data models section, adding @Schema and validation annotations (@NotBlank, @Min) | Restrictions: Use Java records, follow existing DTO patterns, ensure all fields have descriptions and examples | _Leverage: Existing Cart and Cart.LineItem models for structure reference | Success: All DTOs compile, have proper annotations, follow project conventions | Instructions: Mark in-progress [-], create all 4 DTOs, verify compilation, mark complete [x]_

- [x] 8. Create CartMapper utility
  - Files: `src/main/java/com/sivalabs/bookstore/web/mapper/CartMapper.java`
  - Create mapper class to convert Cart ↔ CartDto
  - Add static methods for bidirectional mapping
  - Purpose: Separate domain models from API DTOs
  - _Leverage: Existing ProductMapper pattern from catalog module_
  - _Requirements: Requirement 2 (Shopping Cart REST API)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Mapping Specialist | Task: Create CartMapper class following Requirement 2 with static methods to map Cart↔CartDto and Cart.LineItem↔CartItemDto, following ProductMapper pattern from catalog module | Restrictions: Use static methods for stateless mapping, handle null cases safely, calculate subtotals correctly | _Leverage: ProductMapper from catalog.mappers package as reference | Success: Mapper methods work correctly, handle edge cases, follow existing mapper patterns | Instructions: Mark in-progress [-], create mapper, add unit tests, mark complete [x]_

- [x] 9. Create CartRestController
  - Files: `src/main/java/com/sivalabs/bookstore/web/CartRestController.java`
  - Implement REST endpoints: POST /api/cart/items, PUT /api/cart/items/{code}, GET /api/cart, DELETE /api/cart
  - Add OpenAPI annotations (@Tag, @Operation, @ApiResponses)
  - Purpose: Provide REST API for cart operations
  - _Leverage: CartUtil, ProductApi, CartMapper, HttpSession_
  - _Requirements: Requirement 2 (Shopping Cart REST API)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring REST API Developer | Task: Create CartRestController following Requirement 2 and design.md Component 3 specification, implementing 4 endpoints with proper HTTP methods, status codes, and OpenAPI documentation | Restrictions: Use @RestController, return ResponseEntity, reuse CartUtil and ProductApi, do not duplicate business logic | _Leverage: CartUtil for session management, ProductApi for product lookup, CartMapper for DTO conversion | Success: All 4 endpoints work correctly, proper HTTP status codes (200, 201, 404), comprehensive OpenAPI docs | Instructions: Mark in-progress [-], implement controller, test endpoints, mark complete [x]_

## Task 4: Create Orders REST API

- [x] 10. Create OrdersRestController
  - Files: `src/main/java/com/sivalabs/bookstore/orders/web/OrdersRestController.java`
  - Implement REST endpoints: POST /api/orders, GET /api/orders, GET /api/orders/{orderNumber}
  - Add OpenAPI annotations and gRPC error handling
  - Purpose: Provide REST API for order operations via gRPC
  - _Leverage: OrdersRemoteClient (existing gRPC client)_
  - _Requirements: Requirement 3 (Orders REST API with gRPC)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring REST + gRPC Integration Developer | Task: Create OrdersRestController following Requirement 3 and design.md Component 4 specification, implementing 3 endpoints that delegate to OrdersRemoteClient with proper gRPC error mapping | Restrictions: Use @RestController, delegate all logic to OrdersRemoteClient, map gRPC StatusRuntimeException to HTTP codes (NOT_FOUND→404, INVALID_ARGUMENT→400, UNAVAILABLE→503) | _Leverage: OrdersRemoteClient for all order operations, existing DTOs (CreateOrderRequest, OrderDto, OrderView) | Success: All endpoints work, gRPC calls succeed, errors mapped correctly, OpenAPI docs complete | Instructions: Mark in-progress [-], implement controller, test gRPC integration, mark complete [x]_

- [x] 11. Add @Schema annotations to Orders DTOs
  - Files:
    - `src/main/java/com/sivalabs/bookstore/orders/api/OrderDto.java`
    - `src/main/java/com/sivalabs/bookstore/orders/api/OrderView.java`
    - `src/main/java/com/sivalabs/bookstore/orders/api/CreateOrderRequest.java`
    - `src/main/java/com/sivalabs/bookstore/orders/api/CreateOrderResponse.java`
    - `src/main/java/com/sivalabs/bookstore/orders/api/model/Customer.java`
    - `src/main/java/com/sivalabs/bookstore/orders/api/model/OrderItem.java`
  - Add @Schema annotations to all fields with descriptions and examples
  - Purpose: Document Orders data structures in OpenAPI spec
  - _Leverage: Existing DTO record definitions_
  - _Requirements: Requirement 3 (Orders REST API)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: API Documentation Specialist | Task: Add comprehensive @Schema annotations to all Orders-related DTOs following Requirement 3, documenting all fields with descriptions, examples, and constraints | Restrictions: Do not modify record structures, only add annotations, ensure examples match business domain (bookstore) | _Leverage: Existing DTO records in orders.api package and orders.api.model package | Success: All Orders DTOs fully documented in OpenAPI spec, examples are realistic, constraints are clear | Instructions: Mark in-progress [-], add annotations to 6 files, verify in Swagger, mark complete [x]_

## Task 5: Implement Unified Error Handling

- [x] 12. Create ErrorResponse DTO
  - Files: `src/main/java/com/sivalabs/bookstore/common/models/ErrorResponse.java`
  - Create record with status, message, timestamp fields
  - Add @Schema annotations for OpenAPI documentation
  - Purpose: Standardize error response format across all APIs
  - _Leverage: Common models package structure_
  - _Requirements: Requirement 4 (Error Handling)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java DTO Developer | Task: Create ErrorResponse record following Requirement 4 and design.md error handling specification, with fields for HTTP status code, error message, and timestamp, fully annotated with @Schema | Restrictions: Use Java record, place in common.models package, ensure timestamp uses LocalDateTime, include example values in @Schema | _Leverage: Existing common.models package for placement | Success: ErrorResponse compiles, has all required fields, properly documented for OpenAPI | Instructions: Mark in-progress [-], create DTO, verify in Swagger, mark complete [x]_

- [x] 13. Create OrdersRestExceptionHandler
  - Files: `src/main/java/com/sivalabs/bookstore/orders/web/OrdersRestExceptionHandler.java`
  - Implement @RestControllerAdvice with @ExceptionHandler methods
  - Handle OrderNotFoundException, InvalidOrderException, StatusRuntimeException
  - Map exceptions to HTTP codes and ErrorResponse
  - Purpose: Unified error handling for Orders REST API
  - _Leverage: Error handling patterns from OrdersWebController.handleGrpcStatusException()_
  - _Requirements: Requirement 3, Requirement 4 (Error Handling)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring Exception Handling Specialist | Task: Create OrdersRestExceptionHandler following Requirement 4 and design.md Component 5 specification, implementing @ExceptionHandler methods for domain exceptions and gRPC errors with proper HTTP status mapping | Restrictions: Use @RestControllerAdvice(assignableTypes = OrdersRestController.class), return ResponseEntity<ErrorResponse>, do not expose internal details in error messages | _Leverage: OrdersWebController.handleGrpcStatusException() for gRPC error mapping patterns | Success: All exceptions handled correctly, proper HTTP codes (404, 400, 503), ErrorResponse format consistent | Instructions: Mark in-progress [-], implement handlers, test error scenarios, mark complete [x]_

- [x] 14. Enhance CatalogExceptionHandler for REST API
  - Files: `src/main/java/com/sivalabs/bookstore/catalog/web/CatalogExceptionHandler.java`
  - Update existing @ControllerAdvice to handle both @Controller and @RestController
  - Return ErrorResponse for REST endpoints, error view for Web endpoints
  - Purpose: Support both HTML and JSON error responses
  - _Leverage: Existing CatalogExceptionHandler implementation_
  - _Requirements: Requirement 4 (Error Handling)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring Exception Handling Specialist | Task: Enhance existing CatalogExceptionHandler following Requirement 4 to support both Web (HTML) and REST (JSON) error responses based on request Accept header or path pattern (/api/* → JSON) | Restrictions: Do not break existing Web error handling, detect request type properly (check Accept header or path), maintain backward compatibility | _Leverage: Existing CatalogExceptionHandler logic and ProductNotFoundException handling | Success: Web requests get HTML errors, REST requests get JSON ErrorResponse, no breaking changes | Instructions: Mark in-progress [-], update handler, test both paths, mark complete [x]_

## Task 6: Testing and Validation

- [x] 15. Create REST API integration tests
  - Files: `src/test/java/com/sivalabs/bookstore/api/RestApiIntegrationTests.java`
  - Write @SpringBootTest tests for all REST endpoints
  - Test Products, Cart, Orders APIs with real HTTP requests
  - Purpose: Verify end-to-end REST API functionality
  - _Leverage: Existing integration test patterns, TestContainers_
  - _Requirements: All requirements_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Integration Test Engineer with Spring Boot expertise | Task: Create comprehensive REST API integration tests following all requirements, using @SpringBootTest and TestRestTemplate to test Products, Cart, and Orders endpoints with real HTTP requests | Restrictions: Use TestContainers for PostgreSQL and RabbitMQ, test both success and error paths, ensure tests are isolated and repeatable | _Leverage: Existing integration test setup with Testcontainers, TestRestTemplate for HTTP calls | Success: All REST endpoints tested, both success and error cases covered, tests pass consistently | Instructions: Mark in-progress [-], write tests, ensure 100% endpoint coverage, mark complete [x]_

- [x] 16. Create OpenAPI specification validation tests
  - Files: `src/test/java/com/sivalabs/bookstore/api/OpenApiSpecificationTests.java`
  - Load OpenAPI JSON from /api-docs, validate against OpenAPI 3.0 schema
  - Verify all endpoints, DTOs, and error responses are documented
  - Purpose: Ensure OpenAPI specification completeness
  - _Leverage: OpenAPI validation libraries (swagger-parser)_
  - _Requirements: Requirement 5 (API Documentation)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: API Specification Test Engineer | Task: Create OpenAPI specification validation tests following Requirement 5, loading spec from /api-docs endpoint and validating completeness (all endpoints documented, all DTOs have schemas, error responses included) | Restrictions: Use swagger-parser library for validation, assert all endpoints are present, verify schema completeness | _Leverage: swagger-parser-v3 library for OpenAPI validation | Success: OpenAPI spec validates against OpenAPI 3.0 schema, all endpoints and models documented, no missing schemas | Instructions: Mark in-progress [-], write validation tests, verify spec completeness, mark complete [x]_

- [x] 17. Create CartRestController unit tests
  - Files: `src/test/java/com/sivalabs/bookstore/web/CartRestControllerTests.java`
  - Write @WebMvcTest tests with mocked ProductApi and HttpSession
  - Test all cart endpoints (add, update, get, delete)
  - Purpose: Verify cart REST API logic in isolation
  - _Leverage: Existing @WebMvcTest patterns, MockMvc_
  - _Requirements: Requirement 2 (Cart REST API)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Unit Test Engineer with Spring MVC testing expertise | Task: Create unit tests for CartRestController following Requirement 2, using @WebMvcTest to test all endpoints with mocked dependencies (ProductApi, HttpSession) | Restrictions: Use @WebMvcTest(CartRestController.class), mock all dependencies, test both success and validation error paths | _Leverage: Existing @WebMvcTest patterns from catalog module, MockMvc for HTTP simulation | Success: All cart endpoints tested, proper HTTP codes verified, validation tested, mocks verify interactions | Instructions: Mark in-progress [-], write tests, achieve >90% coverage, mark complete [x]_

- [x] 18. Create OrdersRestController unit tests
  - Files: `src/test/java/com/sivalabs/bookstore/orders/web/OrdersRestControllerTests.java`
  - Write @WebMvcTest tests with mocked OrdersRemoteClient
  - Test all order endpoints and gRPC error mapping
  - Purpose: Verify orders REST API and error handling
  - _Leverage: Existing @WebMvcTest patterns, MockMvc_
  - _Requirements: Requirement 3 (Orders REST API)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Unit Test Engineer with gRPC mocking expertise | Task: Create unit tests for OrdersRestController following Requirement 3, using @WebMvcTest to test all endpoints with mocked OrdersRemoteClient including gRPC error scenarios (UNAVAILABLE→503, NOT_FOUND→404, etc.) | Restrictions: Use @WebMvcTest(OrdersRestController.class), mock OrdersRemoteClient completely, test all gRPC error mappings | _Leverage: Existing @WebMvcTest patterns, MockMvc, StatusRuntimeException for gRPC error simulation | Success: All order endpoints tested, gRPC error mappings verified, >90% coverage achieved | Instructions: Mark in-progress [-], write tests including error cases, mark complete [x]_

## Task 7: Documentation and Finalization

- [x] 19. Generate and test TypeScript SDK
  - Files: `openapi.json` (exported), `frontend-sdk/` (generated)
  - Export OpenAPI spec to JSON, run openapi-generator-cli to generate TypeScript SDK
  - Compile generated SDK and verify types
  - Purpose: Validate OpenAPI spec can generate working client code
  - _Leverage: openapi-generator-cli tool_
  - _Requirements: Requirement 5 (Client SDK Generation)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Frontend Tooling Engineer | Task: Generate TypeScript SDK following Requirement 5 by exporting OpenAPI spec from /api-docs, running openapi-generator-cli with typescript-axios generator, and verifying compilation | Restrictions: Use official openapi-generator-cli, generate to separate directory (frontend-sdk/), verify TypeScript compilation succeeds | _Leverage: openapi-generator-cli tool, npm/yarn for SDK compilation | Success: SDK generates without errors, TypeScript compiles successfully, types match OpenAPI spec | Instructions: Mark in-progress [-], export spec, generate SDK, verify compilation, mark complete [x]_

- [ ] 20. Update README with API documentation
  - Files: `README.md`, `README-API.md` (new)
  - Create API usage documentation with endpoint list, examples, error codes
  - Update main README with links to Swagger UI and API docs
  - Purpose: Provide clear API documentation for developers
  - _Leverage: Existing README structure_
  - _Requirements: Requirement 5 (API Documentation)_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Technical Writer with API documentation expertise | Task: Create comprehensive API documentation following Requirement 5, including endpoint overview, authentication (if any), error codes, and usage examples (curl, JavaScript fetch) in README-API.md, and update main README.md with API section | Restrictions: Follow existing README format, use markdown tables for endpoint list, include practical examples, link to Swagger UI (/swagger-ui.html) | _Leverage: Existing README.md structure and documentation style | Success: API documentation is clear and complete, examples are working, main README updated with API section | Instructions: Mark in-progress [-], create README-API.md, update README.md, mark complete [x]_

- [ ] 21. Verify full system integration
  - Files: N/A (verification task)
  - Start application, verify all REST and Web endpoints work
  - Test session sharing between Web and REST (add via Web, get via REST API)
  - Verify OpenAPI spec completeness in Swagger UI
  - Purpose: Final integration verification
  - _Leverage: All implemented components_
  - _Requirements: All requirements_
  - _Prompt: Implement the task for spec openapi-rest-api, first run spec-workflow-guide to get the workflow guide then implement the task: Role: QA Engineer with system integration expertise | Task: Perform complete system integration verification following all requirements, testing REST+Web coexistence, session sharing, OpenAPI completeness, and end-to-end user flows | Restrictions: Test both Web UI (Thymeleaf) and REST API paths, verify no conflicts, ensure session persists across both interfaces | _Leverage: Running application on localhost:8080, Swagger UI at /swagger-ui.html, existing Web UI | Success: All endpoints accessible, Web and REST coexist without issues, session sharing works, OpenAPI spec complete | Instructions: Mark in-progress [-], test all integration points, document any issues found, mark complete [x] when all verified_
