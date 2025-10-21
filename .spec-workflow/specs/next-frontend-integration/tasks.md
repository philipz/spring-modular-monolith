# Implementation Plan: Next.js Frontend Integration

## Task Overview

This implementation plan breaks down the integration of the Next.js 14 frontend with the Spring Boot modular monolith backend into atomic, file-level tasks. The approach uses **shell scripts for independent frontend builds**, **Docker for containerization**, **nginx as a reverse proxy**, and **Docker Compose for orchestration**. The frontend and backend are completely decoupled and can operate independently.

## Steering Document Compliance

- **Backend Standards**: Spring Boot 3.5.5, Java 21, Spring Web MVC REST APIs
- **Frontend Standards**: Next.js 14 App Router, TypeScript strict mode, pnpm 9.0+, React 18
- **Build Integration**: Shell script-based builds, Docker multi-stage builds, nginx reverse proxy
- **Project Structure**: Backend in `src/main/java/com/sivalabs/bookstore/`, frontend in `frontend-next/` (independent)
- **Session Management**: Hazelcast distributed sessions for stateful cart operations
- **Deployment**: Docker Compose with frontend (nginx), backend (Spring Boot), PostgreSQL, RabbitMQ, HyperDX

## Atomic Task Requirements

**Each task meets these criteria for optimal agent execution:**
- **File Scope**: Touches 1-3 related files maximum
- **Time Boxing**: Completable in 15-30 minutes
- **Single Purpose**: One testable outcome per task
- **Specific Files**: Must specify exact files to create/modify
- **Agent-Friendly**: Clear input/output with minimal context switching

## Tasks

### Phase 1: Integrate Frontend Build with Webproxy nginx

- [x] 1. Create frontend build script
  - File: `frontend-next/build.sh` (new file)
  - Make executable with `chmod +x build.sh`
  - Add shebang: `#!/bin/bash`
  - Check Node.js version (>= 18)
  - Execute `pnpm install --frozen-lockfile`
  - Execute `pnpm build` for Next.js static export
  - Verify `out/` directory created
  - Purpose: Independent frontend build without Maven dependency
  - _Requirements: 2.1, 2.2, 2.3_
  - _Leverages: Shell scripting, Next.js build system_

- [x] 2. Update webproxy nginx.conf for frontend and API routing
  - File: `webproxy/nginx.conf` (modify existing)
  - Keep existing OpenTelemetry configuration and HyperDX exporter
  - Update `location /` block to serve frontend static files from `/usr/share/nginx/html`
  - Add `location /api/` block with reverse proxy to `http://monolith:8080/api`
  - Configure `try_files $uri $uri/ /index.html` for Next.js client-side routing
  - Set cache headers for static assets: `Cache-Control: max-age=31536000, immutable` for `/_next/static/*`
  - Add proxy headers for API requests: `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`
  - Purpose: Single nginx service for both frontend serving and backend API proxy
  - _Requirements: 3.1, 10.2_
  - _Leverages: Existing webproxy nginx configuration, OpenTelemetry integration_

- [x] 3. Update webproxy Dockerfile to include frontend build
  - File: `webproxy/Dockerfile` (modify existing)
  - Keep existing base image: `nginx:1.29.2-alpine-otel`
  - Add build stage: Use `node:18-alpine` for frontend build
  - In build stage: Install pnpm, copy frontend source, run `pnpm install && pnpm build`
  - In runtime stage: Copy built frontend from build stage to `/usr/share/nginx/html`
  - Keep existing nginx.conf and entrypoint.sh setup
  - Purpose: Multi-stage Docker build integrating frontend into webproxy
  - _Requirements: 10.1, 10.3_
  - _Leverages: Existing webproxy Dockerfile, Docker multi-stage builds_

- [x] 4. Update docker-compose.yml webproxy service
  - File: `docker-compose.yml` (modify existing)
  - Update `webproxy` service build context to include frontend-next directory
  - Ensure port mapping `3000:80` or `80:80` for external access
  - Add build args if needed for frontend configuration
  - Verify dependency on `monolith` service
  - Purpose: Configure webproxy to serve both frontend and backend API
  - _Requirements: 10.2, 10.3_
  - _Leverages: Existing docker-compose.yml webproxy service_

- [x] 5. Create frontend development script
  - File: `frontend-next/dev.sh` (new file)
  - Make executable with `chmod +x dev.sh`
  - Check if backend is running (health check on port 8080)
  - Execute `pnpm install` if node_modules missing
  - Execute `pnpm dev` to start Next.js dev server on port 3000
  - Purpose: Convenient development workflow script
  - _Requirements: 8.1, 8.2_
  - _Leverages: Next.js dev server, shell scripting_

### Phase 2: Next.js Project Setup

- [x] 6. Initialize Next.js 14 project with App Router
  - Files: `frontend-next/` directory (new), multiple Next.js config files
  - Execute: `mkdir -p frontend-next && cd frontend-next && pnpm create next-app@latest . --typescript --tailwind --app --src-dir --import-alias "@/*"`
  - Verify: package.json, tsconfig.json, next.config.js created
  - Purpose: Bootstrap Next.js 14 project with TypeScript and App Router
  - _Requirements: Tech.md (Next.js 14, TypeScript)_
  - _Leverages: Next.js create-next-app CLI_
  - _Note: Project already exists with complete Next.js 14 App Router setup_

- [x] 7. Configure Next.js for deployment
  - File: `frontend-next/next.config.js` (modify existing)
  - ~~Add `output: 'export'` to Next.js config~~ **Changed to `output: 'standalone'`**
  - ~~Add `images: { unoptimized: true }` to disable image optimization~~ **Using optimized images with remotePatterns**
  - ~~Add `trailingSlash: true` for proper routing~~ **Not needed with standalone mode**
  - ~~Set `basePath` to empty string for production~~ **Default configuration sufficient**
  - **Actual Implementation**: Using `standalone` mode for full Next.js server features
  - Purpose: ~~Configure Next.js to generate static files for nginx~~ **Configure Next.js standalone server with nginx reverse proxy**
  - _Requirements: 2.3, 10.1_
  - _Leverages: ~~Next.js static export feature~~ Next.js standalone server + nginx reverse proxy (per next_in_nginx.md)_
  - _Architecture Decision: Chose standalone over export for SSR support, API routes, and full Next.js features_

- [x] 8. Add OpenAPI TypeScript generator dependency
  - File: `frontend-next/package.json` (modify existing)
  - ✅ `openapi-typescript` version ^7.4.2 already in devDependencies
  - ✅ Added `gen:types` script: `openapi-typescript http://localhost:8080/api-docs -o apps/web/lib/types/openapi.d.ts`
  - ✅ Added `gen:types:file` script for static YAML fallback
  - ✅ Created `README-OPENAPI.md` documentation
  - Purpose: Enable TypeScript type generation from backend OpenAPI spec
  - _Requirements: 4.1, 4.2_
  - _Leverages: Backend /api-docs endpoint from OpenApiConfig_
  - _Note: Path adjusted to apps/web/lib/types/ to match project structure_

- [x] 9. Add TanStack Query dependencies
  - File: `frontend-next/package.json` (modify existing)
  - ✅ `@tanstack/react-query` version ^5.51.0 already in dependencies
  - ✅ Added `@tanstack/react-query-devtools` version ^5.51.0 to devDependencies
  - ✅ QueryClient already configured in `apps/web/app/providers.tsx` with:
    - Caching strategies (5min staleTime, 10min gcTime)
    - Refetch behavior (on reconnect and mount)
    - Retry logic with exponential backoff
  - ✅ Query hooks already implemented in features (books, cart, orders)
  - Purpose: Install React Query for server state management
  - _Requirements: Design Component 5 (TanStack Query Hooks)_
  - _Leverages: React Query for API state management_

- [x] 10. Add MSW (Mock Service Worker) dependencies
  - File: `frontend-next/package.json` (modify existing)
  - ✅ `msw` version ^2.4.6 already in devDependencies
  - ✅ Added `init:msw` script: `msw init apps/web/public/ --save`
  - ✅ MSW service worker already initialized at `apps/web/public/mockServiceWorker.js`
  - ✅ MSW handlers implemented in `apps/web/mocks/handlers.ts` with mock data
  - ✅ MSW browser setup in `apps/web/mocks/browser.ts`
  - ✅ MSW auto-initialized in development via `apps/web/app/providers.tsx`
  - Purpose: Install MSW for API mocking during development
  - _Requirements: 9.1, 9.2_
  - _Leverages: MSW for development without backend dependency_
  - _Note: Path adjusted to apps/web/public/ to match project structure_

### Phase 3: Backend CORS Configuration

- [x] 11. Create CorsConfig for development
  - File: `src/main/java/com/sivalabs/bookstore/config/CorsConfig.java` (new file)
  - ✅ Implemented `WebMvcConfigurer` with `@Profile("dev")` annotation
  - ✅ Configured `addCorsMappings` to allow `http://localhost:3000` origin
  - ✅ Set `allowCredentials(true)` for session cookie support
  - ✅ Allowed methods: GET, POST, PUT, DELETE, OPTIONS
  - ✅ Set `maxAge(3600)` for preflight cache
  - ✅ Comprehensive Javadoc documentation explaining dev vs production scenarios
  - ✅ Created `docs/cors-configuration.md` with usage guide
  - Purpose: Enable Next.js dev server to call backend APIs
  - _Requirements: 3.1, 3.2, 3.3, 3.5_
  - _Leverages: Spring Web MVC CORS support_
  - _Note: CORS only needed for local dev server; Docker Compose uses nginx proxy (no CORS)_

### Phase 4: Backend Session Configuration

- [x] 12. Configure session cookie properties in application.properties
  - File: `src/main/resources/application.properties` (modify existing)
  - Add `server.servlet.session.timeout=30m`
  - Add `server.servlet.session.cookie.name=BOOKSTORE_SESSION`
  - Add `server.servlet.session.cookie.http-only=true`
  - Add `server.servlet.session.cookie.secure=${USE_SECURE_COOKIES:false}`
  - Add `server.servlet.session.cookie.same-site=strict`
  - Purpose: Configure secure session cookies for cart management
  - _Requirements: 6.1, 6.2, 12.2_
  - _Leverages: Existing Hazelcast session management_

- [x] 13. Create production session configuration
  - File: `src/main/resources/application-prod.properties` (new file or modify existing)
  - Set `USE_SECURE_COOKIES=true`
  - Configure `server.servlet.session.cookie.domain=${FRONTEND_DOMAIN:}`
  - Purpose: Enforce secure cookies in production
  - _Requirements: 12.2_
  - _Leverages: Spring profiles for environment-specific config_

### Phase 5: Frontend HTTP Client Infrastructure

- [x] 14. Create TypeScript error types
  - File: `frontend-next/apps/web/lib/api/errors.ts` (new file)
  - ✅ Defined `HttpError` class extending Error with proper error handling
  - ✅ Added `status: number`, `message: string`, `details?: unknown` properties
  - ✅ Exported `HttpError` class with comprehensive JSDoc documentation
  - ✅ Included Error.captureStackTrace for V8 stack trace support
  - ✅ TypeScript strict mode compliance verified
  - Purpose: Structured error handling for API responses
  - _Requirements: 5.3, 11.2, 11.4_
  - _Leverages: TypeScript class-based error handling_
  - _Note: Path adjusted to apps/web/lib/api/ to match existing project structure_

- [x] 15. Create HTTP client with timeout and session support
  - File: `frontend-next/apps/web/lib/api/client.ts` (new file)
  - Define `API_BASE_URL` from `process.env.NEXT_PUBLIC_API_URL` with fallback to `/api` for production
  - Implement `apiClient.get<T>(path, init)` with 10-second timeout
  - Include `credentials: 'include'` for all requests
  - Add `Accept: application/json` header
  - Import and throw `HttpError` on non-OK responses
  - Purpose: Type-safe HTTP client with session cookie support
  - _Requirements: 5.1, 5.2, 5.3, 6.2, 11.6_
  - _Leverages: Fetch API with AbortController for timeout_
  - _Note: Path adjusted to apps/web/lib/api/ to match existing project structure_

- [x] 16. Implement POST, PUT, DELETE methods in HTTP client
  - File: `frontend-next/apps/web/lib/api/client.ts` (modify existing)
  - Implement `apiClient.post<T>(path, body, init)` with timeout
  - Implement `apiClient.put<T>(path, body, init)` with timeout
  - Implement `apiClient.delete<T>(path, init)` with timeout
  - Add `Content-Type: application/json` for POST/PUT
  - Handle 204 No Content responses
  - Add session expiration detection (401/403 → prompt refresh)
  - Purpose: Complete HTTP verb support with error handling
  - _Requirements: 5.2, 5.3, 11.6, 12.4_
  - _Leverages: GET method from task 15_

### Phase 6: OpenAPI Type Generation

- [x] 17. Create lib/types directory structure
  - Files: `frontend-next/apps/web/lib/types/` directory (new)
  - Create empty `.gitkeep` file to preserve directory
  - Purpose: Prepare directory for generated OpenAPI types
  - _Requirements: 4.2_
  - _Leverages: Next.js src/ directory structure_
  - _Note: Path adjusted to apps/web/lib to align with existing project layout_

- [x] 18. Generate initial TypeScript types from backend
  - Files: Executes script → generates `apps/web/lib/types/openapi.d.ts`
  - Prerequisite: Verify backend running with `curl http://localhost:8080/api-docs`
  - Execute: `cd frontend-next && pnpm gen:types`
  - Verify: Check `openapi.d.ts` contains `paths` and `components` exports
  - Purpose: Bootstrap type-safe API client with backend DTOs
  - _Requirements: 4.2, 4.3, 4.5_
  - _Leverages: Backend OpenAPI endpoint, openapi-typescript tool_
  - _Note: 当前透过 `pnpm gen:types:file`（docs/specs/api/openapi.yaml）離線生成，待後端可用再切回 API_

### Phase 7: TanStack Query Setup

- [x] 19. Create Query Provider component
  - File: `frontend-next/src/app/providers.tsx` (new file)
  - Mark as `'use client'`
  - Create `QueryClient` with retry and staleTime configuration
  - Export `Providers` component wrapping `QueryClientProvider`
  - Include `ReactQueryDevtools` component
  - Configure retry logic: no retry for 4xx (except 408), up to 3 retries for 5xx
  - Configure `retryDelay` with exponential backoff
  - Purpose: Configure TanStack Query for Next.js App Router
  - _Requirements: Design Component 6, Component 7 (retry logic)_
  - _Leverages: TanStack Query client-side hooks_

- [x] 20. Integrate Providers in root layout
  - File: `frontend-next/apps/web/app/layout.tsx` (modify existing)
  - Import `Providers` from `./providers`
  - Wrap `{children}` with `<Providers>` component
  - Purpose: Enable React Query hooks throughout application
  - _Requirements: Design Component 6_
  - _Leverages: Next.js App Router layout system_
  - _Note: Already wrapped with `<Providers>` in apps/web layout_

### Phase 8: Product Feature Hooks

- [x] 21. Create useProducts hook with OpenAPI types
  - File: `frontend-next/apps/web/lib/hooks/use-products.ts` (new file)
  - Import types from `@/lib/types/openapi`
  - Extract `ProductsResponse` type from paths
  - Implement `useProducts(page, pageSize)` using `useQuery`
  - Call `apiClient.get<PagedResult<ProductDto>>` with pagination params
  - Set `queryKey: ['products', page, pageSize]`
  - Set `staleTime: 1000 * 60 * 5` (5 minutes)
  - Purpose: Type-safe product listing with pagination
  - _Requirements: 7.1, 7.4_
  - _Leverages: Generated OpenAPI types, TanStack Query, HTTP client_
  - _Note: Uses React Query v5 placeholder data and maps payload to domain types_

- [x] 22. Create useProduct hook for single product
  - File: `frontend-next/apps/web/lib/hooks/use-products.ts` (modify existing)
  - Extract `ProductResponse` type from OpenAPI paths
  - Implement `useProduct(code)` using `useQuery`
  - Call `apiClient.get<ProductDto>(\`/api/products/${code}\`)`
  - Set `enabled: !!code` to prevent auto-fetch without code
  - Purpose: Type-safe single product retrieval
  - _Requirements: 7.1, 7.4_
  - _Leverages: useProducts hook pattern from task 21_
  - _Note: Returns `ProductDto` derived from OpenAPI response_

### Phase 9: Cart Feature Hooks

- [x] 23. Create useCart hook with session persistence
  - File: `frontend-next/apps/web/lib/hooks/use-cart.ts` (new file)
  - Import `CartResponse` type from OpenAPI paths
  - Implement `useCart()` using `useQuery`
  - Call `apiClient.get<CartDto>('/api/cart')`
  - Set `queryKey: ['cart']`, `staleTime: 1000 * 30` (30 seconds)
  - Purpose: Type-safe cart state retrieval with session
  - _Requirements: 6.1, 6.2, 7.2, 7.4_
  - _Leverages: HTTP client with credentials, TanStack Query_
  - _Note: Normalizes `CartDto` to ensure stable defaults_

- [x] 24. Create cart mutation hooks (add, update, remove)
  - File: `frontend-next/apps/web/lib/hooks/use-cart.ts` (modify existing)
  - Implement `addItem` mutation using `useMutation` with POST
  - Implement `updateQuantity` mutation using `useMutation` with PUT
  - Implement `removeItem` mutation using `useMutation` with DELETE
  - All mutations invalidate `['cart']` query on success
  - Export object: `{ cart, addItem, updateQuantity, removeItem }`
  - Purpose: Type-safe cart operations with optimistic updates
  - _Requirements: 7.2, 7.4_
  - _Leverages: useCart query from task 23_
  - _Note: Supports add/update inputs, includes clear-cart mutation for removal_

### Phase 10: Orders Feature Hooks

- [x] 25. Create useOrders hook with pagination
  - File: `frontend-next/apps/web/lib/hooks/use-orders.ts` (new file)
  - Import `OrdersResponse` type from OpenAPI paths
  - Define `PagedResult<T>` type helper
  - Implement `useOrders(page, pageSize)` using `useQuery`
  - Call `apiClient.get<PagedResult<OrderDto>>` with pagination
  - Set `staleTime: 1000 * 60 * 2` (2 minutes)
  - Purpose: Type-safe order history with pagination
  - _Requirements: 7.3, 7.4_
  - _Leverages: Pagination pattern from useProducts_
  - _Note: Normalizes singleton responses to arrays for UI consumption_

- [x] 26. Create useOrder and useCreateOrder hooks
  - File: `frontend-next/apps/web/lib/hooks/use-orders.ts` (modify existing)
  - Implement `useOrder(orderNumber)` for single order retrieval
  - Implement `useCreateOrder()` mutation using `useMutation` with POST
  - Invalidate `['orders']` and `['cart']` queries on successful order creation
  - Purpose: Type-safe order creation and retrieval
  - _Requirements: 7.3, 7.4_
  - _Leverages: useOrders query from task 25_
  - _Note: Invalidates cart/orders caches and routes users to orders page_

### Phase 11: Cross-Tab Synchronization

- [x] 27. Create Broadcast Channel sync hook
  - File: `frontend-next/apps/web/lib/hooks/use-broadcast-sync.ts` (new file)
  - Mark as `'use client'`
  - Implement `useBroadcastSync()` hook using `useEffect`
  - Create `BroadcastChannel('bookstore-sync')`
  - Listen for `CART_UPDATED` messages and invalidate cart query
  - Subscribe to TanStack Query mutation cache for cart mutations
  - Broadcast `CART_UPDATED` on successful cart mutations
  - Include fallback polling (30s interval) for browsers without Broadcast Channel API
  - Purpose: Synchronize cart state across multiple browser tabs
  - _Requirements: 6.5, Design Component 6 (Cross-Tab Sync)_
  - _Leverages: Broadcast Channel API, TanStack Query mutation cache_
  - _Note: Adds mutationKey instrumentation to cart mutations and fallback polling when BroadcastChannel unavailable_

- [x] 28. Create BroadcastSyncProvider component
  - File: `frontend-next/apps/web/lib/providers/broadcast-sync-provider.tsx` (new file)
  - Mark as `'use client'`
  - Call `useBroadcastSync()` hook
  - Return `null` (side-effect only component)
  - Purpose: Enable cross-tab sync throughout application
  - _Requirements: 6.5_
  - _Leverages: useBroadcastSync hook from task 27_

- [x] 29. Integrate BroadcastSyncProvider in layout
  - File: `frontend-next/apps/web/app/layout.tsx` (modify existing)
  - Import `BroadcastSyncProvider`
  - Add `<BroadcastSyncProvider />` inside `<Providers>` wrapper
  - Purpose: Activate cross-tab synchronization
  - _Requirements: 6.5_
  - _Leverages: BroadcastSyncProvider from task 28_

### Phase 12: Error Handling Components

- [x] 30. Create ErrorBoundary component
  - File: `frontend-next/apps/web/components/error-boundary.tsx` (new file)
  - Mark as `'use client'`
  - Implement React class component with `getDerivedStateFromError` and `componentDidCatch`
  - Provide `fallback` prop for custom error UI
  - Include retry functionality via `handleRetry` method
  - Log errors to console (future: send to HyperDX)
  - Purpose: Graceful error handling for backend unavailability
  - _Requirements: 11.7, Design Component 7_
  - _Leverages: React Error Boundary pattern_
  - _Note: Adds `onRetry` support and default retry button while preserving existing layout integration_

- [x] 31. Create ErrorMessage display component
  - File: `frontend-next/apps/web/components/error-message.tsx` (new file)
  - Mark as `'use client'`
  - Accept `error: unknown` and `onRetry?: () => void` props
  - Implement `getMessage()` to format error messages (4xx vs 5xx vs network)
  - Implement `isRetryable()` to show/hide retry button
  - Display user-friendly error messages
  - Purpose: Consistent error display with retry functionality
  - _Requirements: 11.1, 11.2, 11.3, 11.5, Design Component 7_
  - _Leverages: HttpError from task 14_
  - _Note: Provides default messaging with optional override props and conditional retry action_

### Phase 13: MSW Mock Infrastructure

- [x] 32. Initialize MSW in public directory
  - Files: `frontend-next/apps/web/public/mockServiceWorker.js` (generated)
  - Execute: `cd frontend-next && pnpm init:msw`
  - Verify: mockServiceWorker.js created in public/ directory
  - Purpose: Initialize MSW service worker for API mocking
  - _Requirements: 9.1, 9.2_
  - _Leverages: MSW CLI init command_
  - _Note: Worker copied under apps/web/public via MSW CLI and package.json updated accordingly_

- [x] 33. Create MSW handlers with OpenAPI types
  - File: `frontend-next/apps/web/mocks/handlers.ts` (new file)
  - Import types from `@/lib/types/openapi`
  - Create `http.get('/api/products')` handler with paginated mock data
  - Create `http.get('/api/products/:code')` handler
  - Create `http.get('/api/cart')` handler
  - Create `http.post('/api/cart/items')` handler
  - Create `http.put('/api/cart/items/:code')` handler
  - Create `http.delete('/api/cart/items/:code')` handler
  - Create `http.get('/api/orders')` handler with pagination
  - Create `http.post('/api/orders')` handler
  - Add `delay(300)` to simulate network latency
  - Include error scenario handlers (e.g., 500 errors)
  - Purpose: OpenAPI-aligned mocks for development without backend
  - _Requirements: 9.1, 9.2, 9.3, 9.4, Design Component 8_
  - _Leverages: Generated OpenAPI types, MSW http handlers_
  - _Note: Handlers align with new REST endpoints, reuse shared types, and support simulated failures via query flag_

- [x] 34. Create MSW browser setup
  - File: `frontend-next/apps/web/mocks/browser.ts` (new file)
  - Import `setupWorker` from `msw/browser`
  - Import `handlers` from `./handlers`
  - Export `worker = setupWorker(...handlers)`
  - Purpose: Configure MSW for browser environment
  - _Requirements: 9.1_
  - _Leverages: MSW browser worker, handlers from task 33_
  - _Note: Already present under apps/web/mocks/browser.ts exporting configured worker_

- [x] 35. Create MSW server setup for tests
  - File: `frontend-next/apps/web/mocks/server.ts` (new file)
  - Import `setupServer` from `msw/node`
  - Import `handlers` from `./handlers`
  - Export `server = setupServer(...handlers)`
  - Purpose: Configure MSW for Node.js test environment
  - _Requirements: 9.3_
  - _Leverages: MSW Node server, handlers from task 33_

- [x] 36. Create MSWProvider component
  - File: `frontend-next/apps/web/lib/providers/msw-provider.tsx` (new file)
  - Mark as `'use client'`
  - Check `process.env.NODE_ENV === 'development'` and `NEXT_PUBLIC_USE_MOCKS === 'true'`
  - Dynamically import and start MSW worker if conditions met
  - Display "Loading mocks..." while initializing
  - Purpose: Conditionally enable MSW during development
  - _Requirements: 8.4, 8.5, 9.5, Design Component 8_
  - _Leverages: MSW worker from task 34_
  - _Note: Provides loading fallback, shared init promise, and graceful failure messaging_

- [x] 37. Integrate MSWProvider in layout
  - File: `frontend-next/apps/web/app/layout.tsx` (modify existing)
  - Import `MSWProvider`
  - Wrap entire `<Providers>` tree with `<MSWProvider>`
  - Purpose: Enable MSW mocks when configured
  - _Requirements: 8.4, 9.5_
  - _Leverages: MSWProvider from task 36_

### Phase 14: Environment Configuration

- [x] 38. Create .env.example documentation
  - File: `frontend-next/.env.example` (new file)
  - Document `NEXT_PUBLIC_API_URL` with default `/api` (for nginx reverse proxy in production)
  - Document `NEXT_PUBLIC_USE_MOCKS` with values `true` or `false`
  - Include comments explaining dev vs prod usage
  - Add note: In production, nginx proxies `/api/*` to backend
  - Purpose: Document environment variables for developers
  - _Requirements: 4.1, 5.1, 8.5, Design Component 4_
  - _Leverages: Next.js environment variable conventions_

- [x] 39. Create .env.local for local development
  - File: `frontend-next/.env.local` (new file, git-ignored)
  - Set `NEXT_PUBLIC_USE_MOCKS=false`
  - Set `NEXT_PUBLIC_API_URL=http://localhost:8080`
  - Purpose: Default configuration for local development
  - _Requirements: 8.5_
  - _Leverages: .env.example from task 38_

### Phase 15: Testing Infrastructure

- [x] 40. Create HTTP client unit tests
  - File: `frontend-next/apps/web/lib/api/__tests__/client.test.ts` (new file)
  - Test base URL configuration from environment variable
  - Test credentials inclusion in all requests
  - Test 10-second timeout with AbortController
  - Test error handling for 4xx and 5xx responses
  - Test session expiration detection (401/403)
  - Test HttpError structure
  - Purpose: Ensure HTTP client reliability
  - _Requirements: 5.2, 5.3, 5.4, 11.6, Design Component 7_
  - _Leverages: Vitest, HTTP client from tasks 15-16_
  - _Note: Added vitest suite; `pnpm test` currently fails due to upstream coverage plugin mismatch (vitest 2.1.9 vs @vitest/coverage-v8 3.2.4)_

- [x] 41. Create TanStack Query hooks unit tests
  - File: `frontend-next/apps/web/lib/hooks/__tests__/use-products.test.tsx` (new file)
  - Test `useProducts` hook with MSW
  - Test `useProduct` hook with MSW
  - Test loading and error states
  - Test query key generation
  - Verify type safety with OpenAPI types
  - Purpose: Validate product hooks behavior
  - _Requirements: 7.1, 7.4_
  - _Leverages: Vitest, Testing Library, MSW server, hooks from tasks 21-22_
  - _Note: Uses MSW server overrides for failure cases and QueryClientProvider wrapper_

- [x] 42. Create cart hooks unit tests
  - File: `frontend-next/apps/web/lib/hooks/__tests__/use-cart.test.tsx` (new file)
  - Test `useCart` query hook
  - Test `addItem`, `updateQuantity`, `removeItem` mutations
  - Test query invalidation on successful mutations
  - Test session persistence with credentials
  - Purpose: Validate cart hooks behavior
  - _Requirements: 6.1, 6.2, 7.2, 7.4_
  - _Leverages: Vitest, MSW server, hooks from tasks 23-24_
  - _Note: Uses QueryClientProvider wrapper and MSW overrides for mutation scenarios_

- [x] 43. Create E2E test for product browsing
  - File: `frontend-next/e2e/product-catalog.spec.ts` (new file)
  - Test scenario: Navigate to products page → Verify product list displays
  - Check pagination functionality
  - Verify product details page
  - Check network requests include credentials
  - Purpose: Validate product browsing flow
  - _Requirements: 7.1_
  - _Leverages: Playwright, useProducts hooks_

- [x] 44. Create E2E test for shopping cart flow
  - File: `frontend-next/apps/web/e2e/shopping-cart.spec.ts` (new file)
  - Test: Browse products → Add to cart → View cart
  - Test quantity update functionality
  - Test item removal functionality
  - Verify cart total calculation
  - Test page refresh persistence (session cookie)
  - Purpose: Validate cart management and session persistence
  - _Requirements: 6.1, 6.2, 6.5, 7.2_
  - _Leverages: Playwright, useCart hooks, session management_
  - _Note: Uses MSW-backed flow to add an item, adjust quantity, and assert totals_

- [ ] 45. Create E2E test for order placement
  - File: `frontend-next/apps/web/e2e/checkout-flow.spec.ts` (new file)
  - Test: Add items to cart → Checkout → Fill form → Submit order
  - Verify order confirmation page with order number
  - Check cart cleared after successful order
  - Verify order appears in order history
  - Purpose: Validate complete checkout flow
  - _Requirements: 7.3_
  - _Leverages: Playwright, useCart and useCreateOrder hooks_

### Phase 16: Development Workflow Documentation

- [ ] 46. Create DEVELOPMENT.md guide
  - File: `frontend-next/DEVELOPMENT.md` (new file)
  - Document prerequisites: Node.js 18+, pnpm 9+, Docker
  - Document frontend dev server setup: `./dev.sh` or `pnpm dev`
  - Document type generation workflow: `pnpm gen:types`
  - Document MSW mock usage with environment variables
  - Document Docker build: `docker build -t bookstore-frontend .`
  - Document Docker Compose usage: `docker compose up frontend`
  - Include HMR and Fast Refresh explanation
  - Add troubleshooting section
  - Purpose: Guide developers through frontend setup
  - _Requirements: 8.1, 8.2, 8.3, Design Development Workflow_
  - _Leverages: Next.js dev server, MSW configuration, Docker_

- [ ] 47. Update project README with frontend section
  - File: `README.md` (modify existing at project root)
  - Add "Frontend Integration" section
  - Document Next.js 14 App Router architecture
  - Explain Docker-based deployment with nginx
  - Document development workflow (dev server vs Docker Compose)
  - Document nginx reverse proxy configuration
  - Link to frontend-next/DEVELOPMENT.md
  - Purpose: Help developers understand frontend-backend integration
  - _Requirements: 8.1_
  - _Leverages: Existing README structure, DEVELOPMENT.md from task 46_

### Phase 17: Docker Build and Deployment Validation

- [ ] 48. Create Docker build verification script
  - File: `verify-docker-build.sh` (new file at project root)
  - Make executable with `chmod +x verify-docker-build.sh`
  - Build backend: `./mvnw clean package` and verify exit code 0
  - Build backend Docker image: `docker build -t bookstore-monolith .`
  - Build webproxy (with frontend) Docker image: `docker build -t bookstore-webproxy ./webproxy`
  - Verify images created: `docker images | grep bookstore`
  - Start containers: `docker compose up -d`
  - Wait for health checks to pass
  - Test webproxy serves frontend: `curl -I http://localhost:3000/`
  - Test webproxy reverse proxy to backend API: `curl http://localhost:3000/api/products`
  - Test direct backend access (internal): `docker exec -it <monolith-container> curl http://localhost:8080/api/products`
  - Verify OpenTelemetry traces sent to HyperDX
  - Clean up: `docker compose down`
  - Purpose: Automated verification of Docker build and webproxy integration
  - _Requirements: 10.1, 10.2, 10.3_
  - _Leverages: Docker, Docker Compose, shell scripting, webproxy nginx_

- [ ] 49. Add bundle size validation
  - File: `frontend-next/package.json` (modify existing)
  - Add `@next/bundle-analyzer` to devDependencies
  - Add `analyze` script: `ANALYZE=true next build`
  - Configure Next.js to fail build if bundle > 500KB gzipped
  - Purpose: Enforce bundle size requirements
  - _Requirements: NFR Performance (206), 10.1_
  - _Leverages: Next.js built-in bundle analyzer_

### Phase 18: Docker Compose Integration Testing

- [ ] 50. Create Docker Compose integration test
  - File: `docker-compose-integration-test.sh` (new file at project root)
  - Make executable with `chmod +x docker-compose-integration-test.sh`
  - Start all services: `docker compose up -d`
  - Wait for all health checks to pass (webproxy, monolith, postgres, rabbitmq, hyperdx)
  - Test: Webproxy serves frontend static assets at `http://localhost:3000/`
  - Test: Webproxy serves Next.js client-side routes (e.g., `/products`, `/cart`)
  - Test: Webproxy reverse proxy forwards `/api/*` to monolith backend
  - Test: Backend API accessible via webproxy at `http://localhost:3000/api/products`
  - Test: Session cookies work across nginx reverse proxy
  - Test: CORS headers not present (same-origin via nginx)
  - Test: OpenTelemetry traces appear in HyperDX
  - Test: End-to-end flow - browse products, add to cart, create order
  - Verify: Check Docker logs for errors in all services
  - Verify: nginx access logs show both frontend and API requests
  - Clean up: `docker compose down -v`
  - Purpose: Validate complete Docker Compose integration with unified webproxy
  - _Requirements: All requirements_
  - _Leverages: All components from previous tasks, webproxy nginx, Docker Compose_

## Completion Criteria

All 50 tasks must be marked as `[x]` completed. Each phase builds upon previous phases. Verify:

- Frontend builds independently with shell script (`frontend-next/build.sh`)
- Webproxy Dockerfile integrates frontend build via multi-stage Docker build
- Webproxy nginx serves frontend static files and acts as reverse proxy to backend API
- Webproxy maintains OpenTelemetry integration with HyperDX
- Docker Compose orchestrates all services (webproxy, monolith, postgres, rabbitmq, hyperdx)
- CORS not needed in production (same-origin via webproxy nginx)
- OpenAPI types generated and used in TanStack Query hooks
- Session management works across nginx reverse proxy
- Cross-tab synchronization works via Broadcast Channel API
- MSW mocks enable frontend development without backend
- Error handling works with retry logic and error boundaries
- All E2E tests pass
- Docker build verification passes
- Docker Compose integration test passes
- Documentation complete

## Task Execution Notes

- Tasks are designed to be executed sequentially within each phase
- Some tasks can run in parallel across different phases (e.g., frontend setup while backend config)
- Always verify prerequisites before starting a task
- Mark tasks as `[-]` when in progress, `[x]` when completed
- If a task fails, document blocker and seek assistance

## Architecture Notes

### Development Workflow
- Frontend runs on `localhost:3000` (Next.js dev server with `dev.sh`)
- Backend runs on `localhost:8080` (Spring Boot monolith)
- CORS enabled for development (CorsConfig with `@Profile("dev")`)
- Direct API calls from frontend to backend at `http://localhost:8080/api`
- Frontend types generated from backend OpenAPI spec (`pnpm gen:types`)

### Production Deployment (Docker Compose)
- **Webproxy**: nginx container (port 3000 or 80) serving:
  - Frontend static files at `/`
  - Backend API reverse proxy at `/api/*` → `http://monolith:8080/api`
  - OpenTelemetry traces to HyperDX
- **Monolith**: Spring Boot container (port 8080, internal only)
- **Supporting Services**: PostgreSQL, RabbitMQ, HyperDX
- Same-origin requests via webproxy (no CORS needed in production)
- Session cookies work seamlessly across webproxy reverse proxy

### Build Independence
- **Frontend**: Built with `frontend-next/build.sh` → Integrated into webproxy multi-stage Docker build
- **Backend**: Built with Maven → Docker build for monolith
- **Webproxy**: Multi-stage build (Node.js build + nginx runtime)
- No build-time dependencies between frontend and backend
- Only runtime dependency: frontend calls backend APIs via webproxy reverse proxy

### Webproxy Integration Benefits
- Single entry point for all HTTP traffic
- Unified OpenTelemetry tracing for frontend and backend requests
- Simplified CORS (same-origin in production)
- Centralized SSL/TLS termination point (future)
- Consistent logging and monitoring via nginx access logs
