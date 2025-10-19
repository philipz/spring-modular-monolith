# Requirements Document: Next.js Frontend Integration

## Introduction

This specification defines the requirements for integrating a Next.js 14-based frontend project (to be created in `frontend-next/` directory) with the Spring Boot modular monolith backend. The frontend follows a Specification-Driven Development (SDD) approach with Next.js 14 (App Router), TypeScript, TanStack Query, and OpenAPI-first design. The integration will create a complete full-stack bookstore application that maintains the architectural principles of both the frontend (SDD, type-safety, mocking) and backend (Spring Modulith, event-driven, modular design).

The integration aims to provide:
- Seamless API connectivity between Next.js frontend and Spring Boot backend
- Type-safe API communication using OpenAPI-generated types
- Proper CORS configuration for local development
- Development workflow that maintains both frontend and backend independence
- Production-ready deployment configuration

## Alignment with Product Vision

This feature supports the following goals:
- **Full-Stack Architecture**: Complete the bookstore application with a modern, specification-driven frontend
- **Developer Experience**: Maintain independent frontend/backend development with clear API contracts
- **Type Safety**: Leverage OpenAPI specifications for end-to-end type safety
- **Scalability**: Prepare for future frontend framework migrations or multiple frontend clients
- **Observability**: Integrate frontend with backend's OpenTelemetry tracing

## Requirements

### Requirement 1: API Contract Definition

**User Story:** As a frontend developer, I want a complete OpenAPI specification of the backend APIs, so that I can generate type-safe client code and develop against a well-defined contract

#### Acceptance Criteria

1. WHEN the backend application starts THEN an OpenAPI specification SHALL be available at `/api-docs` endpoint (as configured in Spring Boot application)
2. WHEN the OpenAPI specification is accessed THEN it SHALL provide API documentation for three groups: catalog (`/api/products/**`), cart (`/api/cart/**`), and orders (`/api/orders/**`)
3. WHEN the OpenAPI specification is consumed THEN it SHALL define request/response schemas matching the backend DTOs (ProductDto from catalog module, CartDto/CartItemDto from web layer, OrderDto/CreateOrderRequest/CreateOrderResponse from orders module)
4. WHEN Swagger UI is accessed at `/swagger-ui.html` THEN it SHALL display interactive API documentation for all three API groups
5. IF the backend API changes THEN the OpenAPI specification SHALL automatically reflect those changes through Springdoc OpenAPI runtime generation
6. WHEN the OpenAPI specification is exported for frontend type generation THEN it SHALL be accessible via HTTP GET to `http://localhost:8080/api-docs` endpoint

### Requirement 2: Frontend Build Configuration

**User Story:** As a DevOps engineer, I want the Next.js frontend to be built and served alongside the Spring Boot backend, so that I can deploy a unified application

#### Acceptance Criteria

1. WHEN the Maven build runs THEN it SHALL use frontend-maven-plugin to install Node.js and pnpm, then build the Next.js frontend
2. WHEN the frontend build completes THEN static assets from `.next/standalone` and `.next/static` SHALL be copied to `src/main/resources/static/` directory
3. WHEN the Spring Boot application starts in production mode THEN it SHALL serve the frontend static assets at the root path `/`
4. IF the frontend build fails THEN the Maven build SHALL fail with a non-zero exit code and display the npm/pnpm error output
5. WHEN running in development mode THEN the frontend SHALL remain independently runnable on port 3000 using `pnpm dev`

### Requirement 3: CORS Configuration

**User Story:** As a developer, I want proper CORS configuration during local development, so that I can run frontend (port 3000) and backend (port 8080) independently

#### Acceptance Criteria

1. WHEN the backend runs in development profile THEN it SHALL allow CORS requests from `http://localhost:3000`
2. WHEN CORS is configured THEN it SHALL allow credentials (cookies, sessions) to be sent
3. WHEN CORS is configured THEN it SHALL allow all standard HTTP methods (GET, POST, PUT, DELETE, OPTIONS)
4. IF the backend runs in production profile THEN CORS SHALL be restricted to the production frontend domain
5. WHEN CORS configuration is applied THEN it SHALL support preflight OPTIONS requests

### Requirement 4: API Client Generation

**User Story:** As a frontend developer, I want automatically generated TypeScript API clients, so that I can call backend APIs with full type safety

#### Acceptance Criteria

1. WHEN `pnpm gen:types` is executed THEN it SHALL fetch the OpenAPI specification from `http://localhost:8080/api-docs` endpoint
2. WHEN types are generated THEN they SHALL be placed in `src/lib/types/openapi.d.ts` using openapi-typescript tool
3. WHEN the generated types are imported THEN they SHALL match the backend DTO structures exactly (ProductDto, CartDto, OrderDto, etc.)
4. IF the backend API changes THEN running `pnpm gen:types` SHALL update the frontend types accordingly by re-fetching from `/api-docs`
5. WHEN using generated types THEN TypeScript SHALL enforce compile-time type checking for all API request/response payloads

### Requirement 5: HTTP Client Configuration

**User Story:** As a frontend developer, I want a configured HTTP client that handles authentication and error responses, so that I can make API calls consistently

#### Acceptance Criteria

1. WHEN the HTTP client is configured THEN it SHALL use the backend base URL (configurable via environment variable)
2. WHEN API calls are made THEN the client SHALL include credentials for session management
3. WHEN the backend returns an error response THEN the client SHALL throw typed errors with status codes
4. IF a network error occurs THEN the client SHALL provide meaningful error messages
5. WHEN in development mode THEN the client SHALL connect to `http://localhost:8080` by default

### Requirement 6: Session Management Integration

**User Story:** As a user, I want my shopping cart to persist across page refreshes, so that I don't lose my selections

#### Acceptance Criteria

1. WHEN the user adds items to cart THEN the cart state SHALL be stored in the backend session
2. WHEN the user refreshes the page THEN their cart SHALL be restored from the backend
3. WHEN the backend uses Hazelcast sessions THEN the frontend SHALL work with distributed sessions transparently
4. IF the session expires THEN the frontend SHALL handle session timeout gracefully
5. WHEN multiple browser tabs are open THEN cart updates SHALL be reflected across all tabs

### Requirement 7: Feature Module Integration

**User Story:** As a frontend developer, I want feature modules (books, cart, orders) that integrate with backend APIs, so that I can implement business features

#### Acceptance Criteria

1. WHEN the books catalog feature is implemented THEN it SHALL call `/api/products` endpoint using generated types from ProductDto
2. WHEN the cart feature is implemented THEN it SHALL call `/api/cart/*` endpoints with credentials for session-based cart management
3. WHEN the orders feature is implemented THEN it SHALL call `/api/orders` REST endpoints (note: backend uses gRPC internally but exposes REST API)
4. IF the backend returns paginated results THEN the frontend SHALL handle PagedResult structure with page, pageSize, totalElements, and totalPages
5. WHEN implementing features THEN they SHALL follow the SDD workflow (OpenAPI spec ’ type generation ’ UI spec ’ codegen ’ implementation)

### Requirement 8: Development Workflow

**User Story:** As a developer, I want a smooth development workflow, so that I can iterate quickly on both frontend and backend

#### Acceptance Criteria

1. WHEN running `task start` THEN both frontend dev server (port 3000) and backend (port 8080) SHALL start
2. WHEN frontend code changes THEN hot module replacement SHALL update the UI without full restart
3. WHEN backend code changes THEN Spring Boot DevTools SHALL restart the backend automatically
4. IF the backend is not running THEN the frontend SHALL use MSW mocks for API calls
5. WHEN switching between mock and real API THEN it SHALL be controlled by environment variable

### Requirement 9: MSW Mock Alignment

**User Story:** As a frontend developer, I want MSW mocks that match the backend OpenAPI spec, so that I can develop frontend features before backend APIs are ready

#### Acceptance Criteria

1. WHEN MSW handlers are defined THEN they SHALL match the OpenAPI schema exactly
2. WHEN the backend API changes THEN MSW handlers SHALL be updated to reflect changes
3. WHEN running frontend tests THEN they SHALL use MSW mocks instead of calling real APIs
4. IF MSW mocks return data THEN they SHALL use realistic data structures matching backend entities
5. WHEN MSW is enabled THEN it SHALL intercept API calls at the network level transparently

### Requirement 10: Build and Deployment

**User Story:** As a DevOps engineer, I want a production build that bundles frontend and backend together, so that I can deploy a single artifact

#### Acceptance Criteria

1. WHEN `./mvnw clean package` is executed THEN it SHALL produce a single JAR with embedded frontend assets in `BOOT-INF/classes/static/`
2. WHEN the JAR is deployed THEN the frontend SHALL be accessible at the application root URL `http://host:8080/`
3. WHEN serving production frontend THEN Spring Boot SHALL serve optimized static assets with cache headers: `Cache-Control: max-age=31536000, immutable` for assets with hashes
4. IF static assets are requested THEN they SHALL be served from `/_next/static/*` path as per Next.js conventions
5. WHEN the application starts THEN API endpoints SHALL remain accessible at `/api/*` paths without conflicts

### Requirement 11: Error Handling and Recovery

**User Story:** As a user, I want clear error messages when something goes wrong, so that I understand what happened and what to do next

#### Acceptance Criteria

1. WHEN a network error occurs THEN the frontend SHALL display "Unable to connect. Please check your internet connection." message
2. WHEN the backend returns 400-499 error status THEN the frontend SHALL display the specific error message from the backend response body
3. WHEN the backend returns 500-599 error status THEN the frontend SHALL display "Something went wrong. Please try again later." with error ID for support
4. WHEN an error occurs THEN it SHALL be logged to the browser console with request URL, method, status code, and response body
5. WHEN an error is recoverable (e.g., timeout, 503) THEN the frontend SHALL display a "Retry" button that re-attempts the request
6. WHEN API call exceeds 10 seconds THEN it SHALL timeout and display appropriate error message
7. IF the backend is unreachable during startup THEN the frontend SHALL render with error boundaries and fallback UI

### Requirement 12: Authentication and Authorization

**User Story:** As a user, I want to identify myself to the system, so that I can access personalized features like order history

#### Acceptance Criteria

1. WHEN a user first visits the application THEN they SHALL be assigned an anonymous session for cart management
2. WHEN session management is implemented THEN it SHALL use HttpOnly cookies with Secure and SameSite flags in production
3. WHEN authentication is required (future phase) THEN the frontend SHALL redirect to login page for protected routes
4. IF session expires during usage THEN the frontend SHALL detect expired session and prompt user to refresh
5. WHEN making API calls THEN the frontend SHALL include session cookie automatically via credentials: 'include' option
6. NOTE: Full authentication (login/register) is marked for Phase 2; initial release uses anonymous sessions only

## Assumptions and Constraints

### Assumptions

- Backend OpenAPI endpoints at `/api-docs` are accessible and return valid OpenAPI 3.0 specification
- Backend REST APIs are stable and follow semantic versioning
- Node.js 18+ and pnpm 9.0+ are available in the build environment
- Backend development server runs on `localhost:8080` with CORS enabled for `localhost:3000`
- PostgreSQL, RabbitMQ, and HyperDX services are running when backend starts
- Frontend directory name `frontend-next/` will contain a Next.js 14 application

### Constraints

- Must support modern browsers: Chrome 90+, Firefox 88+, Safari 14+, Edge 90+ (latest 2 major versions)
- Maven build time including frontend build must be under 5 minutes on CI/CD systems
- Cannot introduce breaking changes to existing backend REST API contracts
- Frontend bundle size (initial load) must not exceed 500KB gzipped
- Must maintain backward compatibility with existing Thymeleaf-based UI during transition period
- No server-side rendering (SSR) or static site generation (SSG) in initial release - client-side only

### Out of Scope

- Mobile native applications (iOS/Android) - web responsive UI only
- Real-time collaborative features (e.g., multiple users editing same cart simultaneously)
- Offline-first functionality or Progressive Web App (PWA) features
- Server-side rendering (SSR) for SEO optimization - deferred to Phase 2
- User authentication and registration - Phase 1 uses anonymous sessions only
- Admin dashboard UI - existing Thymeleaf admin pages remain unchanged
- WebSocket-based real-time notifications - Phase 1 uses polling if needed

## Non-Functional Requirements

### Performance
- Frontend bundle size SHALL be less than 500KB initial load (gzipped)
- Largest Contentful Paint (LCP) SHALL be under 2.5 seconds on 4G networks
- First Input Delay (FID) SHALL be under 100ms for user interactions
- Cumulative Layout Shift (CLS) SHALL be under 0.1 to prevent layout jumps
- API response times SHALL be under 200ms for catalog queries (95th percentile)
- API calls SHALL timeout after 10 seconds with clear error message
- Time to Interactive (TTI) SHALL be under 3 seconds on 3G networks
- Backend SHALL handle at least 100 concurrent API requests without degradation
- Static assets SHALL be served with appropriate cache headers (max-age=31536000 for immutable assets with hashes)

### Security
- All API endpoints SHALL enforce HTTPS in production environments
- CORS SHALL be restricted to allowed origins in production
- Session cookies SHALL use `HttpOnly`, `Secure`, and `SameSite=Strict` flags in production
- The OpenAPI documentation endpoint SHALL be protected in production (basic auth or disabled)
- Frontend SHALL never expose sensitive backend configuration or secrets

### Reliability
- Frontend SHALL handle backend unavailability gracefully (error boundaries, fallback UI)
- API client SHALL implement retry logic with exponential backoff for transient failures
- Session management SHALL support distributed sessions via Hazelcast for high availability
- Frontend SHALL display user-friendly error messages for all error scenarios
- Build process SHALL fail fast with clear error messages if dependencies are missing

### Usability
- Loading states SHALL be displayed for all asynchronous operations
- Error messages SHALL be clear and actionable for end users
- Cart updates SHALL provide immediate visual feedback
- Navigation SHALL be intuitive and follow standard e-commerce patterns
- The application SHALL be keyboard accessible (WCAG 2.1 AA compliance)

### Maintainability
- Frontend code SHALL maintain TypeScript strict mode compliance
- Backend API changes SHALL be automatically reflected in frontend types
- The development workflow SHALL support independent frontend/backend iteration
- Build configuration SHALL be documented and reproducible
- Code generation SHALL be repeatable and idempotent

### Browser Compatibility
- The application SHALL function correctly on Chrome 90+, Firefox 88+, Safari 14+, Edge 90+
- The application SHALL display a warning banner on unsupported browsers (IE11, older versions)
- JavaScript SHALL be transpiled to ES2020 for compatibility with target browsers
- CSS SHALL include vendor prefixes for Flexbox and Grid layouts where needed
- Polyfills SHALL be included for missing features (e.g., Promise.allSettled, Intl APIs)
