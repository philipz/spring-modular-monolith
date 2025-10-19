# Implementation Plan: Next.js Frontend Integration

## Task Overview

This implementation plan breaks down the integration of the Next 3 frontend with the Spring Boot modular monolith backend into atomic, file-level tasks. The approach uses shell script-based build orchestration, nginx reverse proxy for production deployment, CORS configuration for development, Next composables with $fetch for API calls, OpenAPI type generation, and Pinia for state management.

## Steering Document Compliance

- **Backend Standards**: Spring Boot 3.5.5, Java 21, Spring Web MVC for configuration
- **Frontend Standards**: Next.js 14 App Router, TypeScript strict mode, pnpm package manager
- **Reverse Proxy**: Nginx 1.29.2 with OpenTelemetry module
- **Project Structure**: Backend config in `src/main/java/com/sivalabs/bookstore/config/`, frontend in `frontend-Next/`, nginx in `webproxy/`, build scripts at project root
- **Build Strategy**: Shell script orchestration, Next static generation, Docker-based nginx deployment

## Atomic Task Requirements
**Each task meets these criteria for optimal agent execution:**
- **File Scope**: Touches 1-3 related files maximum
- **Time Boxing**: Completable in 15-30 minutes
- **Single Purpose**: One testable outcome per task
- **Specific Files**: Must specify exact files to create/modify
- **Agent-Friendly**: Clear input/output with minimal context switching

## Tasks

### Phase 1: Build Infrastructure (Shell Script + Next + Docker)

- [x] 1. Create main build orchestration script build.sh
  - File: `build.sh` (new file at project root)
  - Create shell script that orchestrates Next build → asset copy → Docker webproxy build
  - Add Node.js and pnpm prerequisite checks with clear error messages
  - Include `set -e` for fail-fast behavior
  - Add build verification check for `.output/public` directory existence
  - Purpose: Provide unified build command for Next static generation and webproxy image
  - _Requirements: 2.1, 2.2, 2.3, 2.4_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: DevOps Engineer specializing in build automation and shell scripting | Task: Create main build orchestration script build.sh following requirements 2.1-2.4 that orchestrates Nuxt static generation (pnpm generate), copies assets to webproxy/dist/, and builds Docker webproxy image | Restrictions: Must verify Node.js 18+ and pnpm 9+, use set -e for fail-fast, do not modify frontend-nuxt source code | Leverage: Design document Component 1 (build.sh example), existing webproxy/ directory structure | Success: Script successfully builds Nuxt frontend with `pnpm generate`, copies .output/public/* to webproxy/dist/, builds webproxy:latest Docker image, displays clear success/error messages | Instructions: Edit tasks.md and mark task 1 as [-] in progress, implement the script, test with ./build.sh, verify webproxy:latest image created, then mark task as [x] completed

- [ ] 2. Add frontend-only build utility script
  - File: `build-frontend-only.sh` (new file at project root)
  - Create script that builds Nuxt frontend and copies assets to webproxy/dist/
  - Skips Docker build for faster frontend iteration
  - Purpose: Enable rapid frontend development workflow
  - _Requirements: 2.5_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Frontend Developer specializing in Nuxt build workflows | Task: Create frontend-only build utility script following requirement 2.5 that runs `pnpm generate` and copies assets to webproxy/dist/ | Restrictions: Must verify frontend-nuxt/apps/web/.output/public exists, skip Docker build, maintain same error handling as build.sh | Leverage: Design document Component 1 (build-frontend-only.sh example), build.sh from task 1 | Success: Script builds Nuxt frontend successfully, copies all static assets to webproxy/dist/, runs faster than full build.sh, displays build success message | Instructions: Edit tasks.md and mark task 2 as [-], implement script, test execution, verify assets copied, mark [x] when complete

- [ ] 3. Add clean utility script
  - File: `clean.sh` (new file at project root)
  - Create script that removes all build artifacts (frontend .output, node_modules, webproxy/dist)
  - Purpose: Provide clean slate for fresh builds
  - _Requirements: 2.4_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: DevOps Engineer with build automation expertise | Task: Create clean utility script following requirement 2.4 that removes frontend-nuxt/apps/web/.output, frontend-nuxt/node_modules, and webproxy/dist directories | Restrictions: Must use `rm -rf` safely with explicit paths, do not delete source code or configuration files | Leverage: Design document Component 1 (clean.sh example) | Success: Script safely removes all build artifacts, displays confirmation message, leaves source code intact | Instructions: Mark task 3 as [-], implement script, test with `./clean.sh`, verify artifacts removed, mark [x] complete

- [ ] 4. Make build scripts executable
  - Files: `build.sh`, `build-frontend-only.sh`, `clean.sh`
  - Run `chmod +x` on all three scripts
  - Add scripts to `.gitattributes` with `eol=lf` to prevent Windows line ending issues
  - Purpose: Ensure scripts can be executed on Linux/macOS/Git Bash
  - _Requirements: 2.1_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: DevOps Engineer with cross-platform build experience | Task: Make build scripts executable following requirement 2.1 by setting chmod +x and configuring .gitattributes for Unix line endings | Restrictions: Do not modify script content, ensure .gitattributes only affects shell scripts | Leverage: Existing .gitattributes file if present | Success: All three scripts have executable permissions, .gitattributes ensures LF line endings on all platforms | Instructions: Mark task 4 as [-], run chmod +x, update .gitattributes, verify scripts executable, mark [x] complete

### Phase 2: Nginx Webproxy Configuration

- [ ] 5. Create nginx configuration for static assets and reverse proxy
  - File: `webproxy/nginx.conf` (modify existing)
  - Configure static asset serving from `/usr/share/nginx/html/dist/`
  - Add reverse proxy for `/api/*` requests to `monolith:8080`
  - Configure cache headers (1 year for immutable assets, no-cache for HTML)
  - Set up session cookie proxying with `proxy_set_header Cookie` and `proxy_pass_header Set-Cookie`
  - Purpose: Serve Nuxt frontend and proxy API requests with session support
  - _Requirements: 10.1, 10.2, 10.3, 10.4_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: DevOps Engineer specializing in nginx configuration and reverse proxy | Task: Configure nginx following requirements 10.1-10.4 to serve static assets from /usr/share/nginx/html/dist/ and proxy /api/* to monolith:8080 with proper caching and session cookies | Restrictions: Must preserve OpenTelemetry configuration, use Docker DNS resolver, do not break existing nginx setup | Leverage: Design document Component 6 (nginx.conf example), existing webproxy/nginx.conf | Success: Nginx serves static assets with correct cache headers, proxies API requests with session cookies, OpenTelemetry traces sent to HyperDX | Instructions: Mark task 5 as [-], update nginx.conf, test with docker compose, verify static serving and API proxy, mark [x] complete

- [ ] 6. Update webproxy Dockerfile for static assets
  - File: `webproxy/Dockerfile` (modify existing)
  - Add `COPY dist/ /usr/share/nginx/html/dist/` instruction
  - Ensure nginx.conf is copied via entrypoint.sh
  - Verify base image is `nginx:1.29.2-alpine-otel`
  - Purpose: Package Nuxt static assets into nginx Docker image
  - _Requirements: 10.1, 10.2_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Docker Specialist with nginx container expertise | Task: Update Dockerfile following requirements 10.1-10.2 to copy dist/ directory into nginx html directory | Restrictions: Must use nginx:1.29.2-alpine-otel base image, preserve existing entrypoint.sh configuration | Leverage: Design document Component 6 (Dockerfile example), existing webproxy/Dockerfile | Success: Dockerfile builds successfully, dist/ contents available in /usr/share/nginx/html/dist/, nginx configuration loaded correctly | Instructions: Mark task 6 as [-], update Dockerfile, build image with `docker build -t webproxy:latest webproxy/`, verify image, mark [x] complete

- [ ] 7. Update docker-compose.yml for webproxy service
  - File: `compose.yml` (modify existing)
  - Update or add `webproxy` service using `webproxy:latest` image
  - Expose port 80 as 8080 (map `8080:80`)
  - Add dependency on `monolith` service
  - Set environment variable `HYPERDX_API_KEY` for OpenTelemetry
  - Purpose: Enable Docker Compose deployment with nginx webproxy
  - _Requirements: 10.2, 10.5_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: DevOps Engineer with Docker Compose orchestration experience | Task: Configure webproxy service in compose.yml following requirements 10.2-10.5 with port mapping and dependencies | Restrictions: Do not modify existing services (monolith, postgres, rabbitmq, hyperdx), preserve network configuration | Leverage: Design document Architecture diagram, existing compose.yml services | Success: Webproxy service defined with correct image, ports, dependencies, and environment variables | Instructions: Mark task 7 as [-], update compose.yml, test with `docker compose up`, verify webproxy accessible on :8080, mark [x] complete

### Phase 3: Backend Configuration (CORS, Session, Static Resources)

- [ ] 8. Create CORS configuration for development
  - File: `src/main/java/com/sivalabs/bookstore/config/CorsConfig.java` (new file)
  - Implement `WebMvcConfigurer` with `@Profile("dev")`
  - Configure CORS to allow `http://localhost:3000` with credentials
  - Allow GET, POST, PUT, DELETE, OPTIONS methods
  - Set max age to 3600 seconds for preflight cache
  - Purpose: Enable Nuxt dev server (port 3000) to call backend API (port 8080)
  - _Requirements: 3.1, 3.2, 3.3, 3.5_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Backend Developer with Spring MVC and CORS expertise | Task: Create CORS configuration following requirements 3.1-3.5 for development profile with localhost:3000 origin and credentials support | Restrictions: Must use @Profile("dev"), do not enable CORS in production, follow existing Spring config patterns | Leverage: Design document Component 2 (CorsConfig.java example), existing Spring Web MVC configuration | Success: CORS allows localhost:3000 with credentials, preflight requests succeed, only active in dev profile | Instructions: Mark task 8 as [-], create CorsConfig.java, test with Nuxt dev server, verify CORS headers, mark [x] complete

- [ ] 9. Add session cookie configuration properties
  - File: `src/main/resources/application.properties` (modify existing)
  - Add session timeout: `server.servlet.session.timeout=30m`
  - Configure session cookie name: `server.servlet.session.cookie.name=BOOKSTORE_SESSION`
  - Set HttpOnly flag: `server.servlet.session.cookie.http-only=true`
  - Configure secure flag with default: `server.servlet.session.cookie.secure=${USE_SECURE_COOKIES:false}`
  - Set SameSite policy: `server.servlet.session.cookie.same-site=strict`
  - Purpose: Secure session cookies for anonymous cart management
  - _Requirements: 6.1, 6.2, 12.2_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Backend Security Engineer with session management expertise | Task: Configure session cookie properties following requirements 6.1-6.2 and 12.2 with HttpOnly, Secure, and SameSite flags | Restrictions: Do not modify Hazelcast configuration, preserve existing session settings | Leverage: Design document Component 2.5 (session configuration example), existing application.properties | Success: Session cookies configured with security flags, 30-minute timeout, Hazelcast session store unchanged | Instructions: Mark task 9 as [-], update application.properties, test session creation, verify cookie flags, mark [x] complete

- [ ] 10. Create production session cookie configuration
  - File: `src/main/resources/application-prod.properties` (new file or modify existing)
  - Set secure cookies: `USE_SECURE_COOKIES=true`
  - Configure production domain: `server.servlet.session.cookie.domain=${FRONTEND_DOMAIN}`
  - Purpose: Enforce secure cookies in production environment
  - _Requirements: 12.2_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: DevOps Engineer with production security configuration experience | Task: Create production session configuration following requirement 12.2 with secure cookies and domain settings | Restrictions: Do not hardcode domain values, use environment variable for FRONTEND_DOMAIN | Leverage: Design document Component 2.5 (application-prod.properties example), existing profile-based config | Success: Production profile enforces secure cookies, domain configurable via environment variable | Instructions: Mark task 10 as [-], create/update application-prod.properties, verify prod profile activation, mark [x] complete

### Phase 4: Frontend HTTP Client Enhancement (Nuxt Composables)

- [ ] 11. Create TypeScript error types
  - File: `frontend-nuxt/apps/web/lib/errors.ts` (new file)
  - Define `ApiError` interface with status, message, details fields
  - Create `HttpError` class extending Error with status code and details
  - Purpose: Provide structured error handling for API responses
  - _Requirements: 5.3, 11.2, 11.3, 11.4_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: TypeScript Developer with error handling expertise | Task: Create error types following requirements 5.3 and 11.2-11.4 with ApiError interface and HttpError class | Restrictions: Must extend native Error class, include proper TypeScript types, do not add framework dependencies | Leverage: Design document Component 3 (error types example) | Success: ApiError interface and HttpError class defined with proper types and properties | Instructions: Mark task 11 as [-], create errors.ts, add type definitions, test compilation, mark [x] complete

- [ ] 12. Enhance HTTP client with API base URL
  - File: `frontend-nuxt/apps/web/lib/http.ts` (modify existing)
  - Add `API_BASE_URL` constant from `process.env.NUXT_PUBLIC_API_URL` with fallback logic
  - Fallback: empty string in production, `http://localhost:8080` in development
  - Update GET method to use `${API_BASE_URL}${path}` and include `credentials: 'include'`
  - Add `Accept: application/json` header
  - Purpose: Configure HTTP client for environment-aware API calls
  - _Requirements: 5.1, 5.2, 6.2_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Frontend Developer with Nuxt environment configuration experience | Task: Update HTTP client following requirements 5.1-5.2 and 6.2 with environment-based API URL and credentials | Restrictions: Must preserve existing { data: T } wrapper for backward compatibility, do not break existing usages | Leverage: Design document Component 3 (Enhanced HTTP client example), existing http.ts | Success: API_BASE_URL configured correctly for dev/prod, credentials included in requests, Accept header set | Instructions: Mark task 12 as [-], update http.ts, test with dev server, verify env variable usage, mark [x] complete

- [ ] 13. Add error handling to HTTP client
  - File: `frontend-nuxt/apps/web/lib/http.ts` (modify existing)
  - Import `HttpError` from errors.ts
  - Wrap error responses in HttpError with status code and parsed message
  - Handle JSON parsing errors with fallback to statusText
  - Return direct type `T` for new methods (not `{ data: T }`)
  - Purpose: Provide consistent error handling across all HTTP methods
  - _Requirements: 5.3, 5.4, 11.2, 11.4_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Frontend Developer with HTTP client and error handling expertise | Task: Add error handling to HTTP client following requirements 5.3-5.4 and 11.2-11.4 using HttpError class | Restrictions: Must handle both JSON and non-JSON error responses, maintain backward compatibility for existing code | Leverage: HttpError from task 11, Design document Component 3 (error handling example) | Success: All HTTP errors wrapped in HttpError, error messages parsed from response, fallback to statusText works | Instructions: Mark task 13 as [-], update http.ts, test error scenarios, verify HttpError usage, mark [x] complete

- [ ] 14. Implement POST, PUT, DELETE methods in HTTP client
  - File: `frontend-nuxt/apps/web/lib/http.ts` (modify existing)
  - Add POST method with Content-Type, credentials, and error handling
  - Add PUT method similar to POST with appropriate method
  - Add DELETE method similar to GET with method override
  - Handle 204 No Content responses in POST/PUT
  - Purpose: Complete HTTP verb support for RESTful API calls
  - _Requirements: 5.2, 5.3, 11.6_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Full-stack Developer with REST API client expertise | Task: Implement POST, PUT, DELETE methods following requirements 5.2-5.3 and 11.6 with credentials and error handling | Restrictions: Must include Content-Type for POST/PUT, handle 204 responses, maintain consistent error handling | Leverage: Enhanced GET method from task 13, Design document Component 3 (HTTP client example) | Success: POST, PUT, DELETE methods work correctly with proper headers, credentials, and error handling | Instructions: Mark task 14 as [-], add methods to http.ts, test CRUD operations, mark [x] complete

### Phase 5: OpenAPI Type Generation

- [ ] 15. Update package.json with type generation scripts
  - File: `frontend-nuxt/package.json` (modify existing)
  - Add `gen:types:local` script: `openapi-typescript http://localhost:8080/api-docs -o apps/web/lib/types/openapi.d.ts`
  - Add `gen:types` script with `OPENAPI_SOURCE` environment variable support
  - Keep existing scripts if compatible
  - Purpose: Enable type generation from running backend or spec file
  - _Requirements: 4.1, 4.2, 4.4, 4.5_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Frontend Build Engineer with npm/pnpm scripts expertise | Task: Update package.json scripts following requirements 4.1-4.2 and 4.4-4.5 for OpenAPI type generation | Restrictions: Must preserve existing scripts, verify openapi-typescript is in devDependencies | Leverage: Design document Component 4 (package.json scripts example), existing package.json | Success: gen:types:local and gen:types scripts defined and working, openapi-typescript dependency present | Instructions: Mark task 15 as [-], update package.json, verify scripts executable, mark [x] complete

- [ ] 16. Create environment variable documentation
  - File: `frontend-nuxt/.env.example` (new file)
  - Document `NUXT_PUBLIC_API_URL` with default `http://localhost:8080`
  - Document `OPENAPI_SOURCE` with options: HTTP URL or file path
  - Add comments explaining dev vs prod usage
  - Include example values
  - Purpose: Provide clear documentation for configuration options
  - _Requirements: 4.1, 5.1, 8.5_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Technical Writer with environment configuration documentation experience | Task: Create .env.example following requirements 4.1, 5.1, and 8.5 documenting all environment variables | Restrictions: Use Nuxt environment variable conventions (NUXT_PUBLIC_ prefix), provide clear comments | Leverage: Design document Component 4 (env example) | Success: .env.example file created with all required environment variables and clear documentation | Instructions: Mark task 16 as [-], create .env.example, document variables with examples, mark [x] complete

- [ ] 17. Generate initial TypeScript types from backend
  - Files: Executes script → generates `apps/web/lib/types/openapi.d.ts`
  - Prerequisite check: Verify backend running with `curl http://localhost:8080/api-docs`
  - Execute: `cd frontend-nuxt && pnpm gen:types:local`
  - Verify output: Check `apps/web/lib/types/openapi.d.ts` exists and contains `paths` export
  - Purpose: Bootstrap type-safe API client with backend DTOs
  - _Requirements: 4.2, 4.3, 4.5_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Frontend Developer with OpenAPI and TypeScript expertise | Task: Generate initial types following requirements 4.2-4.3 and 4.5 by running gen:types:local script | Restrictions: Backend must be running on :8080, skip gracefully if backend unavailable with warning | Leverage: gen:types:local script from task 15 | Success: openapi.d.ts file generated with paths and components exports, types match backend DTOs | Instructions: Mark task 17 as [-], start backend, run pnpm gen:types:local, verify generated file, mark [x] complete

### Phase 6: Nuxt Composables with Generated Types

- [ ] 18. Create product composables with OpenAPI types
  - File: `frontend-nuxt/apps/web/composables/useProducts.ts` (new file)
  - Import types from `@/lib/types/openapi`
  - Extract `ProductsResponse` and `ProductResponse` types from paths
  - Implement `useProducts(page)` composable using `$fetch` with credentials
  - Implement `useProduct(code)` composable with auto-fetch on mount
  - Purpose: Provide type-safe product catalog queries
  - _Requirements: 7.1, 7.4_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Nuxt Developer with Composition API and composables expertise | Task: Create product composables following requirements 7.1 and 7.4 using Nuxt $fetch and OpenAPI types | Restrictions: Must use $fetch with credentials: 'include', handle loading and error states, use Nuxt auto-imports | Leverage: Generated types from task 17, Design document Component 5 (useProducts example) | Success: useProducts and useProduct composables work with type safety, loading/error states handled, credentials included | Instructions: Mark task 18 as [-], create useProducts.ts, implement composables, test in page, mark [x] complete

- [ ] 19. Create cart composables with OpenAPI types
  - File: `frontend-nuxt/apps/web/composables/useCart.ts` (new file)
  - Import cart types from OpenAPI paths: `CartDto`, `CartItemDto`
  - Implement `useCart()` composable for fetching cart state
  - Implement `useAddToCart()` composable using $fetch POST
  - Implement `useUpdateCartItem()` and `useRemoveFromCart()` composables
  - Purpose: Provide type-safe cart management with session persistence
  - _Requirements: 6.1, 6.2, 7.2, 7.4, 12.5_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Vue Developer with state management and API integration expertise | Task: Create cart composables following requirements 6.1-6.2, 7.2, 7.4, and 12.5 with session-based cart management | Restrictions: Must include credentials in all $fetch calls, handle cart state updates, provide loading/error states | Leverage: Generated types from task 17, Design document Component 5 (composables pattern) | Success: Cart composables manage cart state with session persistence, type-safe operations, proper state management | Instructions: Mark task 19 as [-], create useCart.ts, implement all cart operations, test cart flow, mark [x] complete

- [ ] 20. Create order composables with OpenAPI types
  - File: `frontend-nuxt/apps/web/composables/useOrders.ts` (new file)
  - Import order types: `CreateOrderRequest`, `CreateOrderResponse`, `OrderDto`
  - Implement `useCreateOrder()` composable for order placement
  - Implement `useOrder(orderNumber)` composable for order details
  - Purpose: Provide type-safe order creation and retrieval
  - _Requirements: 7.3, 7.4_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Frontend Developer with form handling and API integration expertise | Task: Create order composables following requirements 7.3 and 7.4 for order creation and retrieval | Restrictions: Must validate order request before submission, handle success/error responses, clear cart after successful order | Leverage: Generated types from task 17, Design document Component 5 (composables pattern) | Success: Order composables handle order creation and retrieval with type safety, form validation, proper state management | Instructions: Mark task 20 as [-], create useOrders.ts, implement composables, test order flow, mark [x] complete

### Phase 7: Pinia Store Integration (Optional Alternative)

- [ ] 21. Create products Pinia store
  - File: `frontend-nuxt/apps/web/stores/products.ts` (new file)
  - Define products store using Pinia with OpenAPI types
  - Implement `fetchProducts(page)` and `fetchProduct(code)` actions
  - Use $fetch with credentials for API calls
  - Purpose: Provide centralized product state management (alternative to composables)
  - _Requirements: 7.1, 7.4_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Vue State Management Specialist with Pinia expertise | Task: Create products Pinia store following requirements 7.1 and 7.4 as alternative to composables-only approach | Restrictions: Use Pinia setup syntax, maintain type safety with OpenAPI types, include credentials in API calls | Leverage: Generated types from task 17, Design document Component 5 (Pinia store example) | Success: Products store manages product state centrally, actions work with type safety, compatible with Nuxt auto-imports | Instructions: Mark task 21 as [-], create products.ts store, implement actions, test in components, mark [x] complete

- [ ] 22. Create cart Pinia store
  - File: `frontend-nuxt/apps/web/stores/cart.ts` (new file)
  - Define cart store with cart state, add/update/remove actions
  - Use OpenAPI cart types for type safety
  - Implement session-based cart persistence via API calls
  - Purpose: Centralized cart state management (alternative to composables)
  - _Requirements: 6.1, 6.2, 7.2, 7.4_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Vue State Management Developer with cart functionality expertise | Task: Create cart Pinia store following requirements 6.1-6.2, 7.2, and 7.4 with session persistence | Restrictions: Must sync with backend session via $fetch, handle optimistic updates, revert on errors | Leverage: Generated types from task 17, Design document Component 5 (Pinia pattern) | Success: Cart store manages cart state with session sync, optimistic updates, proper error handling | Instructions: Mark task 22 as [-], create cart.ts store, implement actions, test cart operations, mark [x] complete

### Phase 8: Testing Infrastructure

- [ ] 23. Create HTTP client unit tests
  - File: `frontend-nuxt/apps/web/lib/http.test.ts` (new file)
  - Test base URL configuration from environment variable
  - Test credentials inclusion in all requests
  - Test error handling for 4xx and 5xx responses
  - Test HttpError structure with status and message
  - Purpose: Ensure HTTP client reliability and error handling
  - _Requirements: 5.2, 5.3, 5.4, 11.2, 11.3, 11.6_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: QA Engineer with Vitest and HTTP testing expertise | Task: Create HTTP client tests following requirements 5.2-5.4 and 11.2-11.6 covering all methods and error scenarios | Restrictions: Must use Vitest, mock fetch API, test all HTTP methods, cover success and error paths | Leverage: Enhanced HTTP client from tasks 12-14, existing Vitest configuration | Success: All HTTP client methods tested, error handling verified, credentials and headers checked | Instructions: Mark task 23 as [-], create http.test.ts, write tests, run pnpm test, mark [x] complete

- [ ] 24. Create integration test for type generation
  - File: `frontend-nuxt/apps/web/lib/types/__tests__/openapi.integration.test.ts` (new file)
  - Test generated types match expected backend DTO structures
  - Verify `ProductDto`, `CartDto`, `OrderDto` types exist with correct fields
  - Test paths object includes expected API endpoints
  - Verify type extraction works correctly
  - Purpose: Ensure type generation produces valid, usable types
  - _Requirements: 4.2, 4.3, 4.5_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: TypeScript Testing Specialist with type validation expertise | Task: Create type generation integration test following requirements 4.2-4.3 and 4.5 validating generated types | Restrictions: Must check type structure, do not make actual API calls, use TypeScript type assertions | Leverage: Generated openapi.d.ts from task 17 | Success: Test verifies all expected types exist with correct structure, paths object validated | Instructions: Mark task 24 as [-], create integration test, run tests, verify type checks, mark [x] complete

- [ ] 25. Create E2E test for product browsing and cart
  - File: `frontend-nuxt/apps/web/e2e/shopping-flow.spec.ts` (new file)
  - Test scenario: Browse products → Add to cart → View cart
  - Verify product list displays correctly
  - Click "Add to Cart" and verify cart count updates
  - Check network requests include credentials
  - Purpose: Validate product browsing and cart addition flow
  - _Requirements: 6.1, 7.1, 7.2_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: QA Automation Engineer with Playwright E2E testing expertise | Task: Create E2E shopping flow test following requirements 6.1, 7.1, and 7.2 covering browse and cart operations | Restrictions: Must use Playwright, test real user interactions, verify network requests with credentials | Leverage: Existing Playwright configuration | Success: E2E test validates product browsing and cart addition with session persistence | Instructions: Mark task 25 as [-], create shopping-flow.spec.ts, write test, run e2e tests, mark [x] complete

- [ ] 26. Add E2E test for cart updates and session persistence
  - File: `frontend-nuxt/apps/web/e2e/shopping-flow.spec.ts` (modify existing)
  - Test cart quantity update functionality
  - Test cart item removal functionality
  - Verify cart state persists after page refresh
  - Verify cart total amount calculation
  - Purpose: Validate cart management and session persistence
  - _Requirements: 6.1, 6.2, 6.5, 7.2_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: E2E Testing Specialist with session management validation expertise | Task: Add cart update tests following requirements 6.1-6.2 and 7.2 with session persistence validation | Restrictions: Must test page refresh persistence, verify quantity updates and removals, check cart calculations | Leverage: Shopping flow test from task 25 | Success: Cart updates tested, session persistence verified across page refreshes | Instructions: Mark task 26 as [-], extend shopping-flow.spec.ts, test cart operations, mark [x] complete

- [ ] 27. Add E2E test for order placement
  - File: `frontend-nuxt/apps/web/e2e/shopping-flow.spec.ts` (modify existing)
  - Test complete order checkout flow
  - Fill order form with customer details and delivery address
  - Submit order and verify API call completes
  - Verify order confirmation page with order number
  - Check cart is cleared after successful order
  - Purpose: Validate complete shopping flow from browse to order
  - _Requirements: 7.3_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: E2E Testing Engineer with checkout flow validation expertise | Task: Add order placement test following requirement 7.3 for complete checkout flow | Restrictions: Must fill all required form fields, verify order response, check cart cleared after order | Leverage: Cart tests from task 26 | Success: Order placement tested end-to-end, confirmation verified, cart cleared | Instructions: Mark task 27 as [-], extend shopping-flow.spec.ts, test full checkout, mark [x] complete

### Phase 9: Documentation and Deployment

- [ ] 28. Create README for build scripts
  - File: `BUILD.md` (new file at project root)
  - Document `./build.sh` usage and prerequisites (Node.js 18+, pnpm 9+, Docker)
  - Explain `./build-frontend-only.sh` for frontend iteration
  - Document `./clean.sh` for cleaning artifacts
  - Include Docker Compose deployment instructions
  - Add troubleshooting section
  - Purpose: Provide clear build instructions
  - _Requirements: 2.1, 8.1, 10.1_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Technical Writer with DevOps documentation expertise | Task: Create BUILD.md following requirements 2.1, 8.1, and 10.1 documenting all build scripts and deployment | Restrictions: Include prerequisites, step-by-step instructions, troubleshooting tips | Leverage: Design document Component 1 (build scripts), existing build scripts from tasks 1-4 | Success: BUILD.md provides complete build documentation with prerequisites, usage, and troubleshooting | Instructions: Mark task 28 as [-], create BUILD.md, document all scripts, mark [x] complete

- [ ] 29. Update main project README with frontend section
  - File: `README.md` (modify existing at project root)
  - Add "Frontend Integration" section explaining Nuxt + nginx architecture
  - Document development workflow: Nuxt dev server vs Docker Compose
  - Link to BUILD.md for build instructions
  - Document environment variables
  - Add Quick Start guide
  - Purpose: Help developers understand frontend-backend integration
  - _Requirements: 8.1, 8.2, 8.3_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Technical Writer with full-stack documentation experience | Task: Update README.md following requirements 8.1-8.3 with frontend integration documentation | Restrictions: Preserve existing README content, add new section, maintain consistent formatting | Leverage: Existing README.md structure, BUILD.md from task 28 | Success: README updated with comprehensive frontend integration documentation and quick start | Instructions: Mark task 29 as [-], update README.md, add frontend section, mark [x] complete

- [ ] 30. Add deployment checklist documentation
  - File: `DEPLOYMENT.md` (new file at project root)
  - Document production build process: `./build.sh` → Docker Compose deployment
  - Checklist: Verify webproxy image, test routes, check cache headers
  - Document environment-specific configuration
  - Include health check endpoints and rollback procedures
  - Purpose: Provide deployment verification steps
  - _Requirements: 10.1, 10.2, 10.5_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: DevOps Documentation Specialist with deployment checklist expertise | Task: Create DEPLOYMENT.md following requirements 10.1-10.2 and 10.5 with deployment verification checklist | Restrictions: Include pre-deployment, deployment, and post-deployment steps, add rollback procedures | Leverage: Design document Production Considerations section | Success: DEPLOYMENT.md provides complete deployment checklist and verification steps | Instructions: Mark task 30 as [-], create DEPLOYMENT.md, document deployment process, mark [x] complete

### Phase 10: Automated Validation Tests

- [ ] 31. Create CORS validation integration test
  - File: `src/test/java/com/sivalabs/bookstore/config/CorsConfigTests.java` (new file)
  - Use `@SpringBootTest` with `dev` profile
  - Test OPTIONS preflight to `/api/products` with `Origin: http://localhost:3000`
  - Verify CORS headers: Allow-Origin, Allow-Credentials, Allow-Methods
  - Purpose: Automated verification of CORS configuration
  - _Requirements: 3.1, 3.2, 3.3, 3.5_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Backend QA Engineer with Spring integration testing expertise | Task: Create CORS integration test following requirements 3.1-3.5 validating dev profile CORS configuration | Restrictions: Must test OPTIONS preflight, verify all CORS headers, use MockMvc or WebTestClient | Leverage: CorsConfig from task 8, existing Spring test infrastructure | Success: CORS test validates localhost:3000 origin, credentials, and allowed methods | Instructions: Mark task 31 as [-], create CorsConfigTests.java, write tests, run ./mvnw test, mark [x] complete

- [ ] 32. Create session persistence integration test
  - File: `src/test/java/com/sivalabs/bookstore/web/SessionPersistenceTests.java` (new file)
  - Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with Testcontainers
  - Test: Add to cart → Extract session cookie → New request with cookie → Verify cart persists
  - Verify `BOOKSTORE_SESSION` cookie has HttpOnly and SameSite flags
  - Verify 30-minute timeout
  - Purpose: Automated validation of session-based cart management
  - _Requirements: 6.1, 6.2, 6.3, 12.1, 12.2_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Integration Testing Specialist with session management validation expertise | Task: Create session persistence test following requirements 6.1-6.3 and 12.1-12.2 with Testcontainers | Restrictions: Must use real HTTP requests, verify cookie flags, test session timeout | Leverage: Session config from tasks 9-10, existing Testcontainers setup | Success: Session persistence validated with cookie security flags and timeout verification | Instructions: Mark task 32 as [-], create SessionPersistenceTests.java, write tests, run integration tests, mark [x] complete

- [ ] 33. Create build verification test script
  - File: `verify-build.sh` (new file at project root)
  - Execute `./build.sh` and verify exit code 0
  - Check Docker image exists: `docker images | grep webproxy:latest`
  - Start services: `docker compose up -d`
  - Poll health endpoint: `curl http://localhost:8080/actuator/health`
  - Test frontend loads: `curl -I http://localhost:8080/`
  - Test API accessible: `curl http://localhost:8080/api/products`
  - Clean up: `docker compose down`
  - Purpose: Automated verification of production build
  - _Requirements: 10.1, 10.2, 10.5_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: DevOps Engineer with build verification automation expertise | Task: Create build verification script following requirements 10.1-10.2 and 10.5 for automated testing | Restrictions: Must verify build success, Docker image creation, service startup, and endpoint accessibility | Leverage: build.sh from task 1, Docker Compose from task 7 | Success: Verification script validates complete build and deployment workflow | Instructions: Mark task 33 as [-], create verify-build.sh, test execution, mark [x] complete

- [ ] 34. Add bundle size validation to frontend
  - File: `frontend-nuxt/package.json` (modify existing)
  - Add `check:bundle-size` script using size-limit or bundlesize
  - Configure size budget: 500KB (gzipped) for initial bundle
  - Add to build pipeline
  - Purpose: Automated enforcement of bundle size requirements
  - _Requirements: NFR Performance (206), 10.1_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Frontend Performance Engineer with bundle optimization expertise | Task: Add bundle size validation following performance requirement 206 and 10.1 | Restrictions: Must fail build if exceeds 500KB gzipped, integrate with existing build scripts | Leverage: Design document Production Considerations (bundle optimization) | Success: Bundle size check enforces 500KB limit, fails build on violation | Instructions: Mark task 34 as [-], add size check, configure limit, test validation, mark [x] complete

- [ ] 35. Create error boundary component
  - File: `frontend-nuxt/apps/web/components/ErrorBoundary.vue` (new file)
  - Create Vue error boundary with fallback UI
  - Display message: "Unable to load application. Please refresh."
  - Include retry button calling `window.location.reload()`
  - Log errors to console
  - Purpose: Graceful handling of backend unavailability
  - _Requirements: 11.7_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Frontend Developer with Vue error handling expertise | Task: Create error boundary component following requirement 11.7 for graceful error handling | Restrictions: Must use Vue onErrorCaptured hook, provide user-friendly UI, log errors for debugging | Leverage: Vue Composition API, Nuxt component patterns | Success: Error boundary catches errors, displays fallback UI, provides retry functionality | Instructions: Mark task 35 as [-], create ErrorBoundary.vue, implement error handling, mark [x] complete

- [ ] 36. Add error boundary to app layout
  - File: `frontend-nuxt/apps/web/app.vue` or `layouts/default.vue` (modify existing)
  - Wrap application root with ErrorBoundary component
  - Ensure error boundary catches initialization errors
  - Purpose: Enable error boundary for all pages
  - _Requirements: 11.7_
  - _Prompt: Implement the task for spec nuxt-frontend-integration, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Nuxt Developer with layout and error handling integration expertise | Task: Integrate error boundary in app layout following requirement 11.7 | Restrictions: Must wrap entire app, do not break existing layout, test with forced errors | Leverage: ErrorBoundary component from task 35, existing Nuxt layout structure | Success: Error boundary integrated in layout, catches all app errors, fallback UI works | Instructions: Mark task 36 as [-], update layout file, test error handling, mark [x] complete

## Completion Criteria

All 36 tasks must be marked as `[x]` completed. Each phase builds upon previous phases. Verify:
- Build scripts work and create webproxy Docker image
- Nginx serves static assets and proxies API requests
- CORS allows development workflow
- OpenAPI types generated and used in composables
- Session management works with cookies
- E2E tests pass
- Documentation complete
