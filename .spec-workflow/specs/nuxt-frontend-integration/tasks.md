# Implementation Plan: Next.js Frontend Integration

## Task Overview

This implementation plan breaks down the integration of the Next.js frontend with the Spring Boot modular monolith backend into atomic, file-level tasks. The approach follows shell script-based build orchestration, CORS configuration for development, enhanced HTTP client with session support, OpenAPI type generation, and production-ready static resource handling.

## Steering Document Compliance

- **Backend Standards**: Spring Boot 3.5.5, Java 21, Spring Web MVC for configuration
- **Frontend Standards**: Next.js 14 App Router, TypeScript strict mode, pnpm package manager
- **Project Structure**: Backend config in `src/main/java/com/sivalabs/bookstore/config/`, frontend in `frontend-nuxt/`, build scripts at project root
- **Build Strategy**: Shell script orchestration maintaining frontend/backend independence, no Maven frontend plugin

## Atomic Task Requirements
**Each task meets these criteria for optimal agent execution:**
- **File Scope**: Touches 1-3 related files maximum
- **Time Boxing**: Completable in 15-30 minutes
- **Single Purpose**: One testable outcome per task
- **Specific Files**: Must specify exact files to create/modify
- **Agent-Friendly**: Clear input/output with minimal context switching

## Tasks

### Phase 1: Build Infrastructure (Shell Script Orchestration)

- [ ] 1. Create main build orchestration script build.sh
  - File: `build.sh` (new file at project root)
  - Create shell script that orchestrates frontend build → asset copy → backend build
  - Add Node.js and pnpm prerequisite checks with clear error messages
  - Include `set -e` for fail-fast behavior
  - Add build verification check for `.next` directory existence
  - Purpose: Provide unified build command that maintains frontend/backend independence
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ] 2. Add frontend-only build utility script
  - File: `build-frontend-only.sh` (new file at project root)
  - Create script that builds frontend and copies assets to backend resources
  - Skips backend Maven build for faster frontend iteration
  - Purpose: Enable rapid frontend development workflow
  - _Requirements: 2.5_

- [ ] 3. Add clean utility script
  - File: `clean.sh` (new file at project root)
  - Create script that removes all build artifacts (frontend .next, node_modules, Maven target)
  - Clean up copied assets in `src/main/resources/static/`
  - Purpose: Provide clean slate for fresh builds
  - _Requirements: 2.4_

- [ ] 4. Make build scripts executable
  - Files: `build.sh`, `build-frontend-only.sh`, `clean.sh`
  - Run `chmod +x` on all three scripts
  - Add scripts to `.gitattributes` with `eol=lf` to prevent Windows line ending issues
  - Purpose: Ensure scripts can be executed on Linux/macOS/Git Bash
  - _Requirements: 2.1_

### Phase 2: Backend Configuration (CORS, Session, Static Resources)

- [ ] 5. Create CORS configuration for development
  - File: `src/main/java/com/sivalabs/bookstore/config/CorsConfig.java` (new file)
  - Implement `WebMvcConfigurer` with `@Profile("dev")`
  - Configure CORS to allow `http://localhost:3000` with credentials
  - Allow GET, POST, PUT, DELETE, OPTIONS methods
  - Set max age to 3600 seconds for preflight cache
  - Purpose: Enable frontend dev server (port 3000) to call backend API (port 8080)
  - _Leverage: Existing Spring Web MVC configuration infrastructure_
  - _Requirements: 3.1, 3.2, 3.3, 3.5_

- [ ] 6. Add session cookie configuration properties
  - File: `src/main/resources/application.properties` (modify existing)
  - Add session timeout: `server.servlet.session.timeout=30m`
  - Configure session cookie name: `server.servlet.session.cookie.name=BOOKSTORE_SESSION`
  - Set HttpOnly flag: `server.servlet.session.cookie.http-only=true`
  - Configure secure flag with default: `server.servlet.session.cookie.secure=${USE_SECURE_COOKIES:false}`
  - Set SameSite policy: `server.servlet.session.cookie.same-site=strict`
  - Purpose: Secure session cookies for anonymous cart management
  - _Leverage: Existing Hazelcast session configuration_
  - _Requirements: 6.1, 6.2, 12.2_

- [ ] 7. Create production session cookie configuration
  - File: `src/main/resources/application-prod.properties` (new file or modify existing)
  - Set secure cookies: `USE_SECURE_COOKIES=true`
  - Configure production domain: `server.servlet.session.cookie.domain=${FRONTEND_DOMAIN}`
  - Purpose: Enforce secure cookies in production environment
  - _Leverage: Existing profile-based configuration pattern_
  - _Requirements: 12.2_

- [ ] 8. Create static resource handler configuration
  - File: `src/main/java/com/sivalabs/bookstore/config/StaticResourceConfig.java` (new file)
  - Implement `WebMvcConfigurer` to add resource handlers
  - Configure `/_next/static/**` path with 365-day cache control (immutable assets)
  - Configure `/favicon.ico`, `/images/**`, `/css/**` with 7-day cache
  - Purpose: Serve Next.js frontend assets from Spring Boot with proper caching
  - _Leverage: Spring Boot's ResourceHandlerRegistry and static resource serving_
  - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [ ] 9. Add SPA routing fallback configuration
  - File: `src/main/java/com/sivalabs/bookstore/config/StaticResourceConfig.java` (modify existing from task 8)
  - Add `spaRoutingConfigurer()` bean with `@Order(Ordered.LOWEST_PRECEDENCE)`
  - Configure view controller to forward all non-API routes to `/index.html`
  - Use pattern `/{spring:[^\\.]*}` to match SPA routes
  - Purpose: Enable client-side routing for Next.js application
  - _Leverage: StaticResourceConfig.java from task 8_
  - _Requirements: 10.5_

### Phase 3: Frontend HTTP Client Enhancement

- [ ] 10. Create TypeScript error types
  - File: `frontend-nuxt/apps/web/lib/errors.ts` (new file)
  - Define `ApiError` interface with status, message, details fields
  - Create `HttpError` class extending Error with status code and details
  - Purpose: Provide structured error handling for API responses
  - _Requirements: 5.3, 11.2, 11.3, 11.4_

- [ ] 11. Enhance HTTP client with base URL and credentials
  - File: `frontend-nuxt/apps/web/lib/http.ts` (modify existing)
  - Add `API_BASE_URL` constant from environment variable with `http://localhost:8080` default
  - Update GET method to use base URL and include `credentials: 'include'`
  - Add `Accept: application/json` header
  - Keep existing `{ data: T }` return type for backward compatibility
  - Purpose: Configure HTTP client for session-based API calls
  - _Leverage: Existing HTTP client structure at http.ts:7-11_
  - _Requirements: 5.1, 5.2, 6.2, 12.5_

- [ ] 12. Add error handling to HTTP client GET method
  - File: `frontend-nuxt/apps/web/lib/http.ts` (modify existing)
  - Import `HttpError` from errors.ts
  - Wrap error responses in HttpError with status code and parsed message
  - Handle JSON parsing errors with fallback to statusText
  - Add 10-second timeout using AbortSignal
  - Purpose: Provide consistent error handling and timeout for GET requests
  - _Leverage: errors.ts module from task 10_
  - _Requirements: 5.3, 5.4, 11.2, 11.4, 11.6_

- [ ] 13. Implement POST method with credentials and error handling
  - File: `frontend-nuxt/apps/web/lib/http.ts` (modify existing)
  - Add POST method with base URL, credentials, and Content-Type header
  - Include error handling with HttpError wrapping
  - Handle 204 No Content responses
  - Add 10-second timeout using AbortSignal
  - Return direct type `T` instead of `{ data: T }` for new code
  - Purpose: Support cart and order creation API calls with session management
  - _Leverage: Enhanced GET method error handling pattern from task 12_
  - _Requirements: 5.2, 5.3, 11.2, 11.6_

- [ ] 14. Add PUT and DELETE methods to HTTP client
  - File: `frontend-nuxt/apps/web/lib/http.ts` (modify existing)
  - Implement PUT method similar to POST with appropriate method and headers
  - Implement DELETE method similar to GET with method override
  - Include same error handling, credentials, and timeout configuration
  - Purpose: Complete HTTP verb support for RESTful API calls
  - _Leverage: Enhanced GET and POST methods from tasks 12-13_
  - _Requirements: 5.2, 5.3, 11.6_

### Phase 4: OpenAPI Type Generation

- [ ] 15. Update package.json with type generation scripts
  - File: `frontend-nuxt/package.json` (modify existing)
  - Preserve existing `gen:types` script that uses docs/specs/api/openapi.yaml
  - Add `gen:types:local` script pointing to `http://localhost:8080/api-docs`
  - Add `gen:types:backend` alias for `gen:types:local` (clarity)
  - Purpose: Enable type generation from both running backend and committed spec file
  - _Leverage: Existing openapi-typescript devDependency at package.json:40_
  - _Requirements: 4.1, 4.2, 4.4, 4.5_

- [ ] 16. Create environment variable documentation
  - File: `frontend-nuxt/.env.example` (new file)
  - Document `NEXT_PUBLIC_API_URL` with default `http://localhost:8080`
  - Document `OPENAPI_SOURCE` with options: HTTP URL or file path
  - Add comments explaining when to use `gen:types:local` (live backend) vs `gen:types` (committed YAML)
  - Include example: `# NEXT_PUBLIC_API_URL=http://localhost:8080`
  - Purpose: Provide clear documentation for configuration options
  - _Requirements: 4.1, 5.1, 8.5_

- [ ] 17. Generate initial TypeScript types from backend
  - Files: Executes script → generates `apps/web/lib/types/openapi.d.ts`
  - Prerequisite check: Verify backend is running with `curl http://localhost:8080/api-docs` (if fails, skip with warning)
  - Execute: `cd frontend-nuxt && pnpm gen:types:local`
  - Verify output: Check `apps/web/lib/types/openapi.d.ts` exists and contains `paths` export
  - Purpose: Bootstrap type-safe API client with backend DTOs
  - _Leverage: gen:types:local script from task 15_
  - _Requirements: 4.2, 4.3, 4.5_

### Phase 5: API Query Hooks Integration

- [ ] 18. Extract OpenAPI types for product endpoints
  - File: `frontend-nuxt/apps/web/features/books/api/queries.ts` (create new file)
  - Import generated types: `import type { paths } from '@/lib/types/openapi'`
  - Extract `ProductsResponse` type from `paths['/api/products']['get']`
  - Extract `ProductResponse` type from `paths['/api/products/{code}']['get']`
  - Create query key factory: `qk.products(params)` and `qk.product(code)`
  - Purpose: Establish type-safe foundation for product queries
  - _Leverage: Generated OpenAPI types from task 17_
  - _Requirements: 4.3, 4.5, 7.1, 7.4_

- [ ] 19. Implement product query hooks
  - File: `frontend-nuxt/apps/web/features/books/api/queries.ts` (modify from task 18)
  - Implement `useProducts(page)` hook using `client.GET` with `ProductsResponse` type
  - Implement `useProduct(code)` hook using `client.GET` with `ProductResponse` type
  - Use query key factory from task 18
  - Purpose: Provide type-safe product catalog queries
  - _Leverage: Enhanced HTTP client from tasks 11-12, query key factory from task 18_
  - _Requirements: 7.1, 7.4_

- [ ] 20. Create cart types and query hook
  - File: `frontend-nuxt/apps/web/features/cart/api/queries.ts` (create new file)
  - Import generated cart types from OpenAPI paths: `CartDto`, `CartItemDto`
  - Extract type from `paths['/api/cart']['get']`
  - Create query key factory: `qk.cart()`
  - Implement `useCart()` hook for fetching cart state with credentials
  - Purpose: Provide type-safe cart state query with session persistence
  - _Leverage: Enhanced HTTP client with credentials from task 11, generated OpenAPI types from task 17_
  - _Requirements: 6.1, 6.2, 7.2, 7.4, 12.5_

- [ ] 21. Implement cart mutation hooks (add and update)
  - File: `frontend-nuxt/apps/web/features/cart/api/queries.ts` (modify from task 20)
  - Implement `useAddToCart()` mutation using `client.POST` to `/api/cart/items`
  - Implement `useUpdateCartItem()` mutation using `client.PUT` to `/api/cart/items/{code}`
  - Invalidate `qk.cart()` query after successful mutations
  - Include credentials in all API calls for session management
  - Purpose: Provide type-safe cart addition and update operations
  - _Leverage: Enhanced HTTP client POST/PUT methods from tasks 13-14, query key from task 20_
  - _Requirements: 7.2, 7.4, 12.5_

- [ ] 22. Implement cart remove mutation hook
  - File: `frontend-nuxt/apps/web/features/cart/api/queries.ts` (modify from task 21)
  - Implement `useRemoveFromCart()` mutation using `client.DELETE` to `/api/cart/items/{code}`
  - Invalidate `qk.cart()` query after successful removal
  - Include credentials for session management
  - Purpose: Complete cart management mutation hooks
  - _Leverage: Enhanced HTTP client DELETE method from task 14, query invalidation pattern from task 21_
  - _Requirements: 7.2, 7.4, 12.5_

- [ ] 23. Create order types and mutation hook
  - File: `frontend-nuxt/apps/web/features/orders/api/queries.ts` (create new file)
  - Import generated order types: `CreateOrderRequest`, `CreateOrderResponse`, `OrderDto`
  - Extract types from OpenAPI paths: `/api/orders` POST and GET endpoints
  - Create query key factory: `qk.orders()`, `qk.order(orderNumber)`
  - Implement `useCreateOrder()` mutation with typed request/response
  - Purpose: Provide type-safe order creation
  - _Leverage: Enhanced HTTP client POST method from task 13, generated OpenAPI types from task 17_
  - _Requirements: 7.3, 7.4_

### Phase 6: MSW Mock Alignment

- [ ] 24. Update product MSW handlers to match backend schema
  - File: `frontend-nuxt/apps/web/mocks/handlers.ts` (modify existing)
  - Import `ProductDto` and `PagedResult` types from generated OpenAPI
  - Update product list handler to return `PagedResult<ProductDto>` structure
  - Update product detail handler to return `ProductDto` structure
  - Use realistic product data matching backend entities
  - Purpose: Align product mocks with backend OpenAPI contract
  - _Leverage: Generated OpenAPI types from task 17_
  - _Requirements: 9.1, 9.2, 9.4_

- [ ] 25. Update cart MSW handlers to match backend schema
  - File: `frontend-nuxt/apps/web/mocks/handlers.ts` (modify existing from task 24)
  - Import `CartDto` and `CartItemDto` types from generated OpenAPI
  - Update cart GET handler to return `CartDto` structure
  - Update cart POST/PUT/DELETE handlers to match backend request/response schemas
  - Use realistic cart data with session simulation
  - Purpose: Align cart mocks with backend OpenAPI contract and session behavior
  - _Leverage: Generated OpenAPI types from task 17_
  - _Requirements: 9.1, 9.2, 9.4_

- [ ] 26. Update order MSW handlers to match backend schema
  - File: `frontend-nuxt/apps/web/mocks/handlers.ts` (modify existing from task 25)
  - Import `CreateOrderRequest`, `CreateOrderResponse`, `OrderDto` types from generated OpenAPI
  - Update order creation handler to match `CreateOrderRequest`/`CreateOrderResponse` schemas
  - Update order list/detail handlers to return `OrderDto` structures
  - Use realistic order data matching backend entities
  - Purpose: Complete MSW mock alignment with backend API contract
  - _Leverage: Generated OpenAPI types from task 17_
  - _Requirements: 9.1, 9.2, 9.3, 9.4_

### Phase 7: Testing Infrastructure

- [ ] 27. Create HTTP client unit tests
  - File: `frontend-nuxt/apps/web/lib/http.test.ts` (new file)
  - Test base URL configuration from `NEXT_PUBLIC_API_URL` environment variable
  - Test credentials inclusion in all requests (GET, POST, PUT, DELETE)
  - Test error handling for 4xx responses (client errors)
  - Test error handling for 5xx responses (server errors)
  - Test HttpError structure with status code and message
  - Test 10-second timeout behavior
  - Purpose: Ensure HTTP client reliability and error handling
  - _Leverage: Existing Vitest configuration, MSW for mocking HTTP requests_
  - _Requirements: 5.2, 5.3, 5.4, 11.2, 11.3, 11.6_

- [ ] 28. Create integration test for type generation workflow
  - File: `frontend-nuxt/apps/web/lib/types/__tests__/openapi.integration.test.ts` (new file)
  - Test that generated types match expected backend DTO structures
  - Verify `ProductDto`, `CartDto`, `OrderDto` types exist and have correct fields
  - Test that `paths` object includes expected API endpoints: `/api/products`, `/api/cart`, `/api/orders`
  - Verify type extraction works: `paths['/api/products']['get']['responses']['200']`
  - Purpose: Ensure type generation produces valid, usable types
  - _Leverage: Generated openapi.d.ts file from task 17_
  - _Requirements: 4.2, 4.3, 4.5_

- [ ] 29. Create E2E test for product browsing and cart addition
  - File: `frontend-nuxt/apps/web/e2e/shopping-flow.spec.ts` (new file)
  - Test scenario: Browse products → Add product to cart
  - Verify product list displays correctly
  - Click "Add to Cart" button and verify cart count updates
  - Check network requests include credentials
  - Purpose: Validate product browsing and cart addition flow
  - _Leverage: Existing Playwright configuration_
  - _Requirements: 6.1, 7.1, 7.2_

- [ ] 30. Add E2E test for cart updates and session persistence
  - File: `frontend-nuxt/apps/web/e2e/shopping-flow.spec.ts` (modify from task 29)
  - Test cart quantity update functionality
  - Test cart item removal functionality
  - Verify cart state persists after page refresh (session management)
  - Verify cart total amount calculates correctly
  - Purpose: Validate cart management and session persistence
  - _Leverage: Shopping flow test from task 29_
  - _Requirements: 6.1, 6.2, 6.5, 7.2_

- [ ] 31. Add E2E test for order placement and confirmation
  - File: `frontend-nuxt/apps/web/e2e/shopping-flow.spec.ts` (modify from task 30)
  - Test complete order checkout flow
  - Fill order form with customer details and delivery address
  - Submit order and verify API call completes
  - Verify order confirmation page displays order number
  - Check cart is cleared after successful order
  - Purpose: Validate complete shopping flow from browsing to order
  - _Leverage: Cart tests from task 30_
  - _Requirements: 7.3_

### Phase 8: Documentation and Deployment

- [ ] 32. Create README for build scripts
  - File: `BUILD.md` (new file at project root)
  - Document `./build.sh` usage and prerequisites (Node.js 18+, pnpm 9+)
  - Explain `./build-frontend-only.sh` for frontend-only iteration
  - Document `./clean.sh` for cleaning build artifacts
  - Include CI/CD integration examples (GitHub Actions, GitLab CI)
  - Add troubleshooting section for common build issues
  - Purpose: Provide clear build instructions for developers and CI/CD
  - _Requirements: 2.1, 8.1, 10.1_

- [ ] 33. Update main project README with frontend integration section
  - File: `README.md` (modify existing at project root)
  - Add section "Frontend Integration" explaining Next.js + Spring Boot architecture
  - Document development workflow: separate dev servers vs production JAR
  - Link to BUILD.md for detailed build instructions
  - Document environment variables: `NEXT_PUBLIC_API_URL`, `OPENAPI_SOURCE`
  - Add Quick Start guide for frontend development
  - Purpose: Help developers understand frontend-backend integration approach
  - _Requirements: 8.1, 8.2, 8.3_

- [ ] 34. Add deployment checklist to documentation
  - File: `DEPLOYMENT.md` (new file at project root)
  - Document production build process: `./build.sh` → JAR deployment
  - Checklist: Verify frontend assets in JAR, test `/` and `/api/*` routes, check cache headers
  - Document environment-specific configuration (CORS domains, secure cookies)
  - Include health check endpoints: `/actuator/health`
  - Add rollback procedures and troubleshooting guide
  - Purpose: Provide deployment verification steps for production releases
  - _Leverage: Existing Actuator health endpoints_
  - _Requirements: 10.1, 10.2, 10.5_

### Phase 9: Automated Validation Tests

- [ ] 35. Create CORS validation integration test
  - File: `src/test/java/com/sivalabs/bookstore/config/CorsConfigTests.java` (new file)
  - Use `@SpringBootTest` with `dev` profile
  - Test OPTIONS preflight request to `/api/products` with `Origin: http://localhost:3000`
  - Verify response includes `Access-Control-Allow-Origin: http://localhost:3000`
  - Verify `Access-Control-Allow-Credentials: true` header present
  - Verify allowed methods include GET, POST, PUT, DELETE
  - Purpose: Automated verification of CORS configuration
  - _Leverage: CorsConfig.java from task 5, existing test infrastructure_
  - _Requirements: 3.1, 3.2, 3.3, 3.5_

- [ ] 36. Create session persistence integration test
  - File: `src/test/java/com/sivalabs/bookstore/web/SessionPersistenceTests.java` (new file)
  - Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with Testcontainers
  - Test sequence: Add item to cart → Extract session cookie → Make new request with same cookie
  - Verify cart state persists with session cookie
  - Verify `BOOKSTORE_SESSION` cookie has HttpOnly and SameSite=Strict flags
  - Verify session timeout is 30 minutes
  - Purpose: Automated validation of session-based cart management
  - _Leverage: Session cookie configuration from tasks 6-7, existing Testcontainers setup_
  - _Requirements: 6.1, 6.2, 6.3, 12.1, 12.2_

- [ ] 37. Create build verification test script
  - File: `verify-build.sh` (new file at project root)
  - Execute `./build.sh` and check exit code is 0
  - Run `jar tf target/*.jar | grep "BOOT-INF/classes/static/_next/"` and verify output not empty
  - Start JAR in background: `java -jar target/*.jar &`
  - Wait for startup (poll `/actuator/health` endpoint)
  - Test frontend loads: `curl -I http://localhost:8080/ | grep "200 OK"`
  - Test API accessible: `curl http://localhost:8080/api/products | jq .`
  - Kill JAR process
  - Purpose: Automated verification of production build
  - _Leverage: build.sh from task 1, StaticResourceConfig from task 8_
  - _Requirements: 10.1, 10.2, 10.5_

- [ ] 38. Add bundle size validation to frontend build
  - File: `frontend-nuxt/package.json` (modify existing)
  - Add `check:bundle-size` script using `size-limit` or similar tool
  - Configure size budget: 500KB (gzipped) for initial bundle
  - Add post-build hook to automatically run bundle size check
  - Purpose: Automated enforcement of bundle size requirements
  - _Requirements: NFR Performance (206), 10.1_

- [ ] 39. Create error boundary component for backend unavailability
  - File: `frontend-nuxt/apps/web/components/ErrorBoundary.tsx` (new file)
  - Create React error boundary component with fallback UI
  - Display user-friendly message: "Unable to load application. Please refresh."
  - Include retry button that calls `window.location.reload()`
  - Log error details to console for debugging
  - Purpose: Graceful handling of backend unavailability during startup
  - _Leverage: React error boundary pattern_
  - _Requirements: 11.7_

- [ ] 40. Add error boundary to root layout
  - File: `frontend-nuxt/apps/web/app/layout.tsx` (modify existing)
  - Wrap application root with ErrorBoundary component from task 39
  - Ensure error boundary catches initialization errors
  - Purpose: Enable error boundary for all pages
  - _Leverage: ErrorBoundary component from task 39_
  - _Requirements: 11.7_
