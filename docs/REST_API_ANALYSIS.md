# Spring Boot REST API Analysis for Next.js Frontend Integration

## Executive Summary

The Spring Boot modular monolith exposes a well-structured REST API with OpenAPI documentation, Hazelcast-based session management, and a clear separation of concerns across modules. The architecture is ready for Next.js frontend integration with proper configuration for CORS, session handling, and API communication patterns.

---

## 1. REST API Endpoints Overview

### Base URL
- **Development**: `http://localhost:8080`
- **Docker/Production**: Served through nginx reverse proxy at `http://localhost:8080`
- **OpenAPI Spec**: `http://localhost:8080/api-docs`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`

### API Endpoints Structure

#### 1.1 Products API (`/api/products`)
**Module**: Catalog

| Endpoint | Method | Description | Response |
|----------|--------|-------------|----------|
| `/api/products` | GET | Get paginated products | `PagedResult<ProductDto>` |
| `/api/products/{code}` | GET | Get product by code | `ProductDto` |

**Query Parameters**:
- `page` (optional, default=1): Page number (1-based)

**Example Response**:
```json
{
  "data": [
    {
      "code": "P100",
      "name": "The Hunger Games",
      "description": "Winning will make you famous. Losing means certain death...",
      "imageUrl": "https://images.sivalabs.in/products/the-hunger-games.jpg",
      "price": 34.0
    }
  ],
  "totalElements": 100,
  "pageNumber": 1,
  "totalPages": 10,
  "isFirst": true,
  "isLast": false,
  "hasNext": true,
  "hasPrevious": false
}
```

**DTOs**:
- `ProductDto`:
  - `code`: String (required, example: "P100")
  - `name`: String (required, example: "The Hunger Games")
  - `description`: String (optional)
  - `imageUrl`: String (optional)
  - `price`: BigDecimal (required)

#### 1.2 Cart API (`/api/cart`)
**Module**: Web (Session-based)

| Endpoint | Method | Description | Request | Response |
|----------|--------|-------------|---------|----------|
| `/api/cart` | GET | Get current cart | - | `CartDto` |
| `/api/cart/items` | POST | Add item to cart | `AddToCartRequest` | `CartDto` |
| `/api/cart/items/{code}` | PUT | Update item quantity | `UpdateQuantityRequest` | `CartDto` |
| `/api/cart` | DELETE | Clear cart | - | 204 No Content |

**Request DTOs**:
```typescript
// AddToCartRequest
{
  code: string;        // Product code, required
  quantity: number;    // Quantity >= 1, required
}

// UpdateQuantityRequest
{
  quantity: number;    // Quantity >= 1, required
}
```

**Response DTO** (`CartDto`):
```typescript
{
  items: CartItemDto[];     // Array of cart items
  totalAmount: BigDecimal;  // Total cart value
  itemCount: number;        // Total items in cart
}

// CartItemDto
{
  code: string;           // Product code
  name: string;           // Product name
  price: BigDecimal;      // Unit price
  quantity: number;       // Quantity (>= 1)
  subtotal: BigDecimal;   // price × quantity
}
```

**Session Management**:
- Uses HTTP Session stored in Hazelcast
- Session ID in `JSESSIONID` cookie
- Session timeout: 30 minutes
- Sticky sessions required (handled by nginx in docker-compose)

#### 1.3 Orders API (`/api/orders`)
**Module**: Orders (via gRPC client wrapper)

| Endpoint | Method | Description | Request | Response |
|----------|--------|-------------|---------|----------|
| `/api/orders` | POST | Create order | `CreateOrderRequest` | `CreateOrderResponse` (201) |
| `/api/orders` | GET | List orders | - | `List<OrderView>` |
| `/api/orders/{orderNumber}` | GET | Get order details | - | `OrderDto` |

**Request DTO** (`CreateOrderRequest`):
```typescript
{
  customer: {
    name: string;        // Required, validated @NotBlank
    email: string;       // Required, validated @Email
    phone: string;       // Required, validated @NotBlank
  };
  deliveryAddress: string;  // Required, validated @NotEmpty
  item: {
    code: string;        // Product code
    name: string;        // Product name
    price: BigDecimal;   // Price
    quantity: number;    // Quantity >= 1
  };
}
```

**Response DTOs**:
```typescript
// CreateOrderResponse (HTTP 201)
{
  orderNumber: string;   // e.g., "ORD-2025-001234"
}

// OrderDto
{
  orderNumber: string;
  item: {
    code: string;
    name: string;
    price: BigDecimal;
    quantity: number;
  };
  customer: {
    name: string;
    email: string;
    phone: string;
  };
  deliveryAddress: string;
  status: "NEW" | "CONFIRMED" | "SHIPPED" | "DELIVERED";
  createdAt: LocalDateTime;
  totalAmount: BigDecimal;  // Computed: price × quantity
}

// OrderView (list response)
{
  orderNumber: string;
  status: string;
  createdAt: LocalDateTime;
}
```

---

## 2. OpenAPI Configuration

### 2.1 Configuration Details
**File**: `src/main/java/com/sivalabs/bookstore/config/OpenApiConfig.java`

- **API Title**: "BookStore REST API"
- **Version**: "1.0.0"
- **License**: Apache 2.0
- **Server URL**: `http://localhost:8080` (configurable)

### 2.2 API Documentation Access
- **OpenAPI JSON**: `GET http://localhost:8080/api-docs`
- **Swagger UI**: `GET http://localhost:8080/swagger-ui.html`
- **Grouped Endpoints**:
  - Catalog: `/api/products/**`
  - Cart: `/api/cart/**`
  - Orders: `/api/orders/**`

### 2.3 OpenAPI Configuration Properties
```properties
# API Documentation
springdoc.api-docs.path=/api-docs
springdoc.api-docs.enabled=true

# Swagger UI
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.try-it-out-enabled=true

# API Grouping
springdoc.group-configs[0].group=catalog
springdoc.group-configs[0].paths-to-match=/api/products/**

springdoc.group-configs[1].group=cart
springdoc.group-configs[1].paths-to-match=/api/cart/**

springdoc.group-configs[2].group=orders
springdoc.group-configs[2].paths-to-match=/api/orders/**

# Actuator endpoints not shown
springdoc.show-actuator=false
```

---

## 3. Session Management

### 3.1 Session Configuration
**File**: `src/main/java/com/sivalabs/bookstore/config/HttpSessionConfig.java`

```properties
# Session Store Type
spring.session.store-type=hazelcast
spring.session.hazelcast.map-name=spring:session:sessions
spring.session.timeout=30m  # 30 minutes
```

### 3.2 Session Behavior
- **Store**: Hazelcast distributed cache
- **Cookie Name**: `JSESSIONID`
- **Cookie Attributes**:
  - HttpOnly: true (default Spring Security setting)
  - SameSite: Lax (default)
  - Secure: false (in development, true in production)
- **Session Replication**: Across all Hazelcast cluster nodes
- **Sticky Sessions**: Handled by nginx proxy

### 3.3 Session Data Flow
1. Client sends request without session → Spring creates new session
2. Session stored in Hazelcast with TTL of 30 minutes
3. Server responds with `Set-Cookie: JSESSIONID=...` header
4. Client includes `Cookie: JSESSIONID=...` in subsequent requests
5. Server retrieves session from Hazelcast using session ID

### 3.4 Cart Data Storage
- Stored in HTTP Session as `Cart` object
- Session key: Configured in `CartUtil` (internal)
- Multi-item support: Currently supports single item (can be extended)
- Scope: Per-user (one cart per session)

---

## 4. CORS Configuration

### 4.1 Current CORS Status
**No explicit CORS configuration found** in Spring application.

### 4.2 CORS Handling Architecture
CORS is handled at the **nginx reverse proxy level** (webproxy):

**File**: `webproxy/nginx.conf`

```nginx
location / {
    proxy_pass http://monolith:8080;
    proxy_set_header Host $http_host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

### 4.3 CORS Setup Required for Next.js

Add CORS configuration to `WebConfig.java`:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")  // Next.js dev server
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)  // Important for session cookies
                .maxAge(3600);
    }
}
```

**Nginx Alternative** (if handling at proxy level):
```nginx
add_header 'Access-Control-Allow-Origin' $http_origin always;
add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
add_header 'Access-Control-Allow-Headers' 'Content-Type' always;
add_header 'Access-Control-Allow-Credentials' 'true' always;
```

---

## 5. Error Handling

### 5.1 Exception Handlers
**Files**:
- `src/main/java/com/sivalabs/bookstore/orders/web/OrdersRestExceptionHandler.java`
- `src/main/java/com/sivalabs/bookstore/catalog/web/CatalogExceptionHandler.java`

### 5.2 Standard Error Response Format

```typescript
interface ErrorResponse {
  status: number;           // HTTP status code (e.g., 404, 400, 500)
  message: string;          // Error message
  timestamp: LocalDateTime; // When error occurred (ISO format)
}
```

### 5.3 Common HTTP Status Codes

| Status | Condition | Example |
|--------|-----------|---------|
| 200 | Success | GET /api/products |
| 201 | Created | POST /api/orders |
| 204 | No Content | DELETE /api/cart |
| 400 | Validation failed | POST /api/orders with invalid email |
| 404 | Resource not found | GET /api/products/INVALID |
| 503 | Service unavailable | Orders service down |
| 504 | Timeout | Request exceeds gRPC deadline |

### 5.4 gRPC Error Mapping
Orders API wraps gRPC calls with error mapping:

```java
switch (status.getCode()) {
    case NOT_FOUND -> HttpStatus.NOT_FOUND (404)
    case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST (400)
    case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE (503)
    case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT (504)
    default -> HttpStatus.INTERNAL_SERVER_ERROR (500)
}
```

---

## 6. Request/Response Patterns

### 6.1 Validation Patterns

All DTOs use Jakarta validation annotations:

```java
@NotBlank(message = "...")    // Reject null/empty strings
@NotEmpty(message = "...")    // Reject null/empty collections
@Email                         // Validate email format
@Min(value = 1)               // Minimum numeric value
@Valid                        // Cascade validation to nested objects
```

**Validation Error Response** (HTTP 400):
```json
{
  "status": 400,
  "message": "Validation failed: customer.email - must be a well-formed email address; ",
  "timestamp": "2025-10-19T12:34:56"
}
```

### 6.2 Request Content-Type
- All endpoints expect/return `application/json`
- Set header: `Content-Type: application/json`

### 6.3 Response Content-Type
- All responses: `application/json`
- Charset: UTF-8

---

## 7. Build and Deployment Integration

### 7.1 Maven Build Structure

**File**: `pom.xml`

Key dependencies for API:
```xml
<!-- REST Support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- OpenAPI Documentation -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.7.0</version>
</dependency>

<!-- Session Management -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-hazelcast</artifactId>
</dependency>
```

### 7.2 Docker Compose Setup

**File**: `compose.yml`

Services relevant to API:

```yaml
monolith:
  image: sivaprasadreddy/spring-modular-monolith:0.0.1-SNAPSHOT
  ports:
    - "8080:8080"  # Spring Boot app
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/postgres
    SPRING_RABBITMQ_HOST: rabbitmq

webproxy:
  image: nginx:latest
  ports:
    - "8080:80"  # External access
  depends_on:
    - monolith
```

### 7.3 Frontend Asset Serving

**Current Architecture**: Thymeleaf templates (for SSR)
- Static assets: `/src/main/resources/static/`
- Templates: `/src/main/resources/templates/`

**For Next.js Integration**:
1. **Option A**: Serve Next.js separately on port 3000
2. **Option B**: Build Next.js as static assets and serve via Spring Boot
   - Build: `pnpm build` → outputs to `.next` directory
   - Serve via nginx or Spring static resource handler

**Recommended**: Option A for development (separate dev servers), Option B for production (single deployment)

---

## 8. Next.js Frontend Integration Checklist

### 8.1 API Client Setup

```typescript
// apps/web/lib/http.ts - Updated for full API support
interface RequestOptions {
  body?: unknown;
  headers?: Record<string, string>;
  credentials?: RequestCredentials;  // Add for cookie support
}

export const client = {
  async GET<T = unknown>(path: string, init?: RequestInit): Promise<{ data: T }> {
    const res = await fetch(path, {
      credentials: 'include',  // Include cookies
      ...init
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return { data: await res.json() };
  },

  async POST<T = unknown>(path: string, options?: RequestOptions): Promise<{ data: T }> {
    const res = await fetch(path, {
      method: 'POST',
      credentials: 'include',  // Include cookies
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
      body: options?.body ? JSON.stringify(options.body) : undefined,
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return { data: await res.json() };
  }
};
```

### 8.2 Environment Configuration

```bash
# .env.local
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### 8.3 API Route Examples

```typescript
// app/api/products/route.ts
export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const page = searchParams.get('page') || '1';
  
  const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/products?page=${page}`, {
    credentials: 'include'
  });
  return res.json();
}

// app/api/cart/route.ts
export async function GET() {
  const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/cart`, {
    credentials: 'include'
  });
  return res.json();
}
```

### 8.4 Session Cookie Handling

Next.js needs to:
1. **Send cookies**: Include `credentials: 'include'` in fetch
2. **Receive cookies**: Browser automatically stores Set-Cookie
3. **Persist cookies**: Between requests in TanStack Query

```typescript
// Configure TanStack Query for cookies
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
    },
  },
});

// Configure fetch wrapper
const fetchWithCredentials = (url: string, options?: RequestInit) =>
  fetch(url, {
    ...options,
    credentials: 'include'  // Always include cookies
  });
```

### 8.5 CORS Configuration Required

Spring Boot needs CORS enabled for localhost:3000:

```java
// Add to WebConfig.java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:3000",   // Development
                    "http://localhost:8080"    // Nginx proxy
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

---

## 9. Key Architectural Patterns

### 9.1 Session vs Token Trade-offs

**Current**: HTTP Session + Hazelcast (Sticky Sessions)
- ✅ Pro: Server-side control, secure, works with traditional backends
- ❌ Con: Requires sticky sessions in load balancing

**Alternative**: JWT Tokens (for stateless scaling)
- Would require different approach: no JSESSIONID cookie
- Each request includes `Authorization: Bearer <token>`

### 9.2 Event-Driven Architecture

Orders module publishes `OrderCreatedEvent`:
- Published to RabbitMQ for external consumption
- Published to Spring Modulith internal event bus
- Next.js can subscribe to webhook if needed

### 9.3 Module Communication

```
Next.js Frontend
    ↓ HTTP/REST
nginx (webproxy)
    ↓
Spring Boot Monolith
    ├→ Catalog Module (REST/gRPC internal)
    ├→ Orders Module (via gRPC client wrapper)
    └→ Hazelcast (sessions, cache)
```

---

## 10. Configuration Reference

### 10.1 Key Application Properties

```properties
# Server
server.port=8080

# Session
spring.session.store-type=hazelcast
spring.session.timeout=30m

# OpenAPI
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres

# Hazelcast
bookstore.cache.enabled=true
```

### 10.2 Environment Variables for Docker

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_RABBITMQ_HOST=rabbitmq
BOOKSTORE_GRPC_CLIENT_TARGET=orders-service:9090
```

---

## 11. Testing Integration Points

### 11.1 API Testing

```bash
# Get products
curl http://localhost:8080/api/products

# Add to cart (requires session)
curl -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -d '{"code":"P100","quantity":1}' \
  -c cookies.txt

# Get cart (with session from cookies)
curl http://localhost:8080/api/cart -b cookies.txt

# Create order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer":{"name":"John","email":"john@example.com","phone":"+1234567890"},
    "deliveryAddress":"123 Main St",
    "item":{"code":"P100","name":"Book","price":29.99,"quantity":1}
  }'
```

### 11.2 OpenAPI Spec Generation

```bash
# Download OpenAPI spec
curl http://localhost:8080/api-docs > openapi.json

# Use in Next.js for type generation
pnpm gen:types
```

---

## 12. Summary: Integration Guide for Next.js

### Quick Start Checklist

- [ ] **CORS Setup**: Add CORS configuration to WebConfig for localhost:3000
- [ ] **API Client**: Update lib/http.ts to include `credentials: 'include'`
- [ ] **Environment**: Set NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
- [ ] **Session Cookies**: Configure TanStack Query to persist JSESSIONID
- [ ] **Error Handling**: Map Spring error responses to Next.js error boundaries
- [ ] **API Routes**: Create Next.js API routes to wrap Spring Boot endpoints (optional)
- [ ] **Testing**: Use OpenAPI spec to generate types: `pnpm gen:types`
- [ ] **Mocking**: Update MSW handlers with actual API endpoints
- [ ] **Documentation**: Reference `/swagger-ui.html` for API exploration

### Key URL References

| Purpose | URL |
|---------|-----|
| API Base | http://localhost:8080 |
| OpenAPI Spec | http://localhost:8080/api-docs |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Products | http://localhost:8080/api/products |
| Cart | http://localhost:8080/api/cart |
| Orders | http://localhost:8080/api/orders |
| Health Check | http://localhost:8080/actuator/health |
| Modulith Info | http://localhost:8080/actuator/modulith |

