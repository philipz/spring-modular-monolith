# BookStore REST API Reference

The Spring Modulith backend exposes a REST surface under `/api/**` for product browsing, cart management, and order lifecycle operations. The API is session-based (Hazelcast-backed) and designed to be consumed by the Next.js storefront as well as external clients.

## Overview

- **Base URL (local):** `http://localhost:8080` (or `http://localhost` when routed through nginx in Docker Compose)
- **OpenAPI spec:** `http://localhost:8080/api-docs`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Authentication:** None (all endpoints are currently open for demo purposes)
- **Session cookie:** `BOOKSTORE_SESSION` (HttpOnly, SameSite=Strict, 30 min TTL)
- **Content type:** JSON (`application/json`); only session cookie is required for stateful operations

To call the API from a browser app (e.g. Next.js) ensure `fetch`/Axios requests use `credentials: 'include'` or `withCredentials: true` so the `BOOKSTORE_SESSION` cookie is sent.

## Endpoints

### Products (`/api/products`)

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/products?page=:page` | Returns a paginated list of products (default page = 1) |
| `GET` | `/api/products/{code}` | Returns a single product by code |

Response payload (`PagedResult<ProductDto>`):

```jsonc
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

```bash
curl -H "Accept: application/json" \
     "http://localhost:8080/api/products?page=1"
```

### Cart (`/api/cart`)

Cart contents are bound to the session (`BOOKSTORE_SESSION`).

| Method | Path | Body | Description |
| --- | --- | --- | --- |
| `GET` | `/api/cart` | – | Retrieve the current cart (empty cart is returned if none exists) |
| `POST` | `/api/cart/items` | `{ "code": "...", "quantity": 1 }` | Add or replace the cart item |
| `PUT` | `/api/cart/items/{code}` | `{ "quantity": 2 }` | Update quantity for the current item |
| `DELETE` | `/api/cart` | – | Clear the cart |

Response (`CartDto`):

```jsonc
{
  "items": [
    {
      "code": "P100",
      "name": "The Hunger Games",
      "price": 34.0,
      "quantity": 1,
      "subtotal": 34.0
    }
  ],
  "totalAmount": 34.0,
  "itemCount": 1
}
```

Example (add an item):

```bash
curl -i -X POST "http://localhost:8080/api/cart/items" \
     -H "Content-Type: application/json" \
     -d '{"code":"P100","quantity":1}'
# => Set-Cookie: BOOKSTORE_SESSION=...
```

### Orders (`/api/orders`)

All order endpoints require the session cookie so the backend can read the cart.

| Method | Path | Body | Description |
| --- | --- | --- | --- |
| `POST` | `/api/orders` | `CreateOrderRequest` | Create a new order (returns `201 Created`) |
| `GET` | `/api/orders` | – | List orders as lightweight summaries (`List<OrderView>`) |
| `GET` | `/api/orders/{orderNumber}` | – | Fetch full order details (`OrderDto`) |

`CreateOrderRequest`:

```jsonc
{
  "customer": {
    "name": "Jane Doe",
    "email": "jane@example.com",
    "phone": "+1-555-0100"
  },
  "deliveryAddress": "123 Main Street, Springfield",
  "item": {
    "code": "P100",
    "name": "The Hunger Games",
    "price": 34.0,
    "quantity": 1
  }
}
```

Successful response (`201`):

```json
{ "orderNumber": "ORD-2025-000123" }
```

## Error Handling

- Validation failures return HTTP `400` with a standard `ErrorResponse` payload.
- Missing resources (`/api/products/{code}`, `/api/orders/{number}`) respond with `404`.
- Integration errors (e.g. gRPC connectivity problems) bubble up as `503` with diagnostic details.

## gRPC Companion

Orders functionality is also exposed via gRPC. The monolith runs a server on port `9091` (`bookstore.grpc.server.port`) and the extracted `orders-service` runs on `9090`. The REST controller delegates to `OrdersGrpcClient`, so REST clients need no extra configuration, but gRPC consumers can connect directly if required.

## Tooling & SDKs

- **TypeScript SDK:** generated via `pnpm gen:types` into `frontend-sdk/` using `openapi-typescript`.
- **API tests:** use `k6.js` for end-to-end load coverage (`k6 run k6.js`).
- **Documentation:** the OpenAPI spec is kept in sync with controller annotations via SpringDoc (`springdoc.*` properties in `application.properties`).

## Checklist for Consumers

1. Include the `BOOKSTORE_SESSION` cookie (use `credentials: 'include'` in browsers).
2. Expect JSON responses; set `Accept: application/json`.
3. Handle `201 Created` (orders) and `204 No Content` (cart delete) semantics.
4. Consult `http://localhost:8080/swagger-ui.html` for live examples while developing.
