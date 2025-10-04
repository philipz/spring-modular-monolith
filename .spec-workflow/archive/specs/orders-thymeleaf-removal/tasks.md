# Tasks: Orders Thymeleaf Removal

- [x] 1. Remove Thymeleaf dependencies from orders/pom.xml
  - File: orders/pom.xml
  - Delete `spring-boot-starter-thymeleaf`, `thymeleaf-layout-dialect`, and `htmx-spring-boot-thymeleaf` dependency entries plus the `htmx-spring-boot-thymeleaf.version` property while keeping other web/API dependencies intact.
  - Ensure the resulting dependency tree has no transitive Thymeleaf artifacts and Maven still resolves successfully.
  - _Leverage: orders/pom.xml, mvn dependency:tree_
  - _Requirements: FR-2, NFR-2_
  - _Prompt: Implement the task for spec orders-thymeleaf-removal, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Build engineer focused on Maven dependency hygiene | Task: Clean orders/pom.xml by removing the three Thymeleaf-related artifacts and their version property, verifying no leftover Thymeleaf jars remain | Restrictions: Do not touch non-Thymeleaf dependencies, keep comments intact, ensure the module still builds | _Leverage: orders/pom.xml, mvn dependency:tree | _Requirements: FR-2, NFR-2 | Success: Maven resolves without Thymeleaf artifacts, pom.xml remains well-formed, dependency tree passes review | Status: Mark this task as [-] before you start it and [x] once complete in tasks.md._

- [x] 2. Purge legacy Thymeleaf templates
  - File: orders/src/main/resources/templates/
  - Remove all HTML files (including partials and layout) and obsolete `.gitkeep` so the module ships without server-rendered views.
  - Confirm no other classpath resources rely on those templates.
  - _Leverage: orders/src/main/resources/templates, git ls-files_
  - _Requirements: FR-1_
  - _Prompt: Implement the task for spec orders-thymeleaf-removal, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring Boot engineer specializing in resource management | Task: Delete every Thymeleaf template under orders/src/main/resources/templates ensuring the directory is removed or left empty as appropriate | Restrictions: Do not touch non-template resources, ensure deletions are reflected in git, keep resource directory structure valid | _Leverage: orders/src/main/resources/templates, git ls-files | _Requirements: FR-1 | Success: No HTML templates remain in the module and builds/tests no longer reference them | Status: Mark this task as [-] before you start it and [x] once complete in tasks.md._

- [x] 3. Remove MVC controllers and form model
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/web/{CartController.java,OrderWebController.java,OrderForm.java}
  - Delete the legacy MVC controllers and `OrderForm` record that only supported Thymeleaf flows, ensuring no references remain.
  - Audit imports/usages to prevent compilation errors after removal.
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/web, git grep 'OrderWebController'
  - _Requirements: FR-3_
  - _Prompt: Implement the task for spec orders-thymeleaf-removal, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring MVC engineer transitioning code to REST | Task: Delete CartController, OrderWebController, and OrderForm, cleaning up any residual references so the module builds without them | Restrictions: Do not remove Cart, CartUtil, or ProductApiAdapter, ensure package-info.java remains | _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/web, git grep 'OrderWebController' | _Requirements: FR-3 | Success: Controllers and form class are gone, compilation succeeds, no warnings about missing beans | Status: Mark this task as [-] before you start it and [x] once complete in tasks.md._

- [x] 4. Introduce cart DTOs and API response records
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/web/dto/
  - Create `CartDto`, `CartItemDto`, `AddToCartRequest`, `UpdateQuantityRequest`, and `ApiResponse<T>` records mirroring the shapes defined in the design.
  - Include mappers or factory helpers as needed to translate between `Cart` and the DTOs.
  - _Leverage: orders/src/main/java/com/sivalabs/bookstore/orders/web/Cart.java, CartUtil.java, design.md §2 Data Transfer Objects_
  - _Requirements: FR-3, NFR-3_
  - _Prompt: Implement the task for spec orders-thymeleaf-removal, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java backend engineer focused on API modeling | Task: Add the DTO package with the five records plus any minimal converters so REST endpoints can serialize cart state without exposing session internals | Restrictions: Keep records immutable, follow existing package naming, reuse BigDecimal types, add concise JavaDoc where helpful | _Leverage: Cart.java, CartUtil.java, design.md §2 | _Requirements: FR-3, NFR-3 | Success: DTOs compile, represent all required fields, and have unit-level mappers ready for use | Status: Mark this task as [-] before you start it and [x] once complete in tasks.md._

- [x] 5. Implement CartRestController with session-backed JSON endpoints
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/web/CartRestController.java
  - Create a REST controller exposing `GET /api/cart`, `POST /api/cart/items`, and `PUT /api/cart/items/{productCode}` that leverages `CartUtil`, `ProductApiAdapter`, and the new DTOs.
  - Ensure HTTP status codes, validation, and empty-cart scenarios match the design expectations.
  - _Leverage: CartUtil.java, ProductApiAdapter.java, OrdersApi.java, design.md §1 CartController → CartRestController_
  - _Requirements: FR-3, AC-2_
  - _Prompt: Implement the task for spec orders-thymeleaf-removal, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring REST engineer with session management expertise | Task: Build CartRestController implementing the three endpoints, mapping request bodies to cart operations, calling OrdersApi when appropriate, and returning DTOs/envelopes | Restrictions: Keep endpoints under `/api/cart`, enforce validation, reuse existing logging style, do not introduce new dependencies | _Leverage: CartUtil.java, ProductApiAdapter.java, OrdersApi.java | _Requirements: FR-3, AC-2 | Success: Endpoints compile, behave per design, and integrate cleanly with session cart state | Status: Mark this task as [-] before you start it and [x] once complete in tasks.md._

- [x] 6. Extend exception handling with cart-specific errors
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/web/OrdersExceptionHandler.java
  - Add cart-specific `ProblemDetail` responses and introduce supporting exceptions (e.g., `CartNotFoundException`, `InvalidCartOperationException`) in an `orders.web.exception` package.
  - Make sure CartRestController throws these exceptions in error paths.
  - _Leverage: OrdersExceptionHandler.java, design.md §3 Error Handling Strategy_
  - _Requirements: FR-3, FR-4_
  - _Prompt: Implement the task for spec orders-thymeleaf-removal, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring exception-handling specialist | Task: Add cart-specific exceptions and map them to ProblemDetail responses consistent with existing handler patterns, wiring them into CartRestController flows | Restrictions: Keep exception classes lightweight, reuse Instant timestamps, maintain consistent titles/details | _Leverage: OrdersExceptionHandler.java, design.md §3 | _Requirements: FR-3, FR-4 | Success: Cart errors return structured ProblemDetails and tests can assert on them | Status: Mark this task as [-] before you start it and [x] once complete in tasks.md._

- [x] 7. Align OrderRestController with REST-only contract
  - File: orders/src/main/java/com/sivalabs/bookstore/orders/web/OrderRestController.java
  - Review and adjust the existing REST endpoints to reflect the removal of Thymeleaf, including optional response envelopes or documentation tweaks so clients rely solely on JSON.
  - Update OpenAPI annotations if response shapes change.
  - _Leverage: OrderRestController.java, design.md §1 OrderWebController → OrderRestController_
  - _Requirements: FR-3, NFR-3_
  - _Prompt: Implement the task for spec orders-thymeleaf-removal, first run spec-workflow-guide to get the workflow guide then implement the task: Role: REST API maintainer | Task: Reconcile OrderRestController with the new contract, ensuring responses and docs match the JSON-first approach and no remnants of view logic persist | Restrictions: Preserve existing URLs and HTTP verbs, keep logging style, stay compatible with OrdersApi | _Leverage: OrderRestController.java, design.md §1 | _Requirements: FR-3, NFR-3 | Success: Controller methods reflect JSON-only flows and documentation matches the new responses | Status: Mark this task as [-] before you start it and [x] once complete in tasks.md._

- [x] 8. Update and extend automated tests for REST flows
  - File: orders/src/test/java/com/sivalabs/bookstore/orders/{web/CartRestControllerTests.java,web/OrderRestControllerTests.java,OrdersEndToEndTests.java}
  - Create WebMvc tests for CartRestController, adjust OrderRestController tests for any response changes, and rework the end-to-end test to validate REST responses instead of HTML pages.
  - Ensure new DTO mappings and error scenarios are covered.
  - _Leverage: existing OrderRestControllerTests, OrdersEndToEndTests, MockMvc utilities_
  - _Requirements: FR-4, AC-3_
  - _Prompt: Implement the task for spec orders-thymeleaf-removal, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Spring test engineer | Task: Build comprehensive REST-focused tests covering cart CRUD, order APIs, and the end-to-end happy path without relying on Thymeleaf pages | Restrictions: Use MockMvc/TestRestTemplate patterns already in place, keep tests deterministic, avoid duplicating fixtures | _Leverage: OrderRestControllerTests, OrdersEndToEndTests | _Requirements: FR-4, AC-3 | Success: Tests assert JSON payloads, cover success and error paths, and replace prior HTML assertions | Status: Mark this task as [-] before you start it and [x] once complete in tasks.md._

- [x] 9. Run formatters and full test suites
  - File: Project root & orders module
  - Execute `./mvnw spotless:apply` at the repo root (if touched root files) and run `./mvnw -ntp verify` in both the root project and the `orders` module to confirm everything passes.
  - Capture or summarize key failures before rerunning if issues arise.
  - _Leverage: ./mvnw spotless:apply, ./mvnw -ntp verify, orders/mvnw -ntp verify_
  - _Requirements: AC-3, AC-4_
  - _Prompt: Implement the task for spec orders-thymeleaf-removal, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Continuous integration guardian | Task: Run formatting and verification commands in root and orders modules, addressing any failures uncovered | Restrictions: Do not skip failing tests, document follow-up actions if something breaks, avoid modifying pom defaults just to pass | _Leverage: ./mvnw spotless:apply, ./mvnw -ntp verify, orders/mvnw -ntp verify | _Requirements: AC-3, AC-4 | Success: Both projects build cleanly with passing tests and formatted code | Status: Mark this task as [-] before you start it and [x] once complete in tasks.md._
