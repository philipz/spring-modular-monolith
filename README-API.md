# BookStore REST API Documentation

Comprehensive REST API documentation for the BookStore modular monolith application.

## Table of Contents

- [Overview](#overview)
- [API Explorer](#api-explorer)
- [Authentication](#authentication)
- [Base URL](#base-url)
- [API Endpoints](#api-endpoints)
  - [Products API](#products-api)
  - [Cart API](#cart-api)
  - [Orders API](#orders-api)
- [Data Models](#data-models)
- [Error Handling](#error-handling)
- [TypeScript SDK](#typescript-sdk)
- [Code Examples](#code-examples)

## Overview

The BookStore REST API provides endpoints for managing products, shopping cart, and orders. Built with Spring Boot and Spring Modulith, it follows a modular monolith architecture with clear module boundaries.

**Key Features:**
- RESTful API design following OpenAPI 3.0 specification
- Session-based shopping cart management
- gRPC backend integration for orders
- Comprehensive error handling
- Type-safe TypeScript SDK available

## API Explorer

### Swagger UI (Interactive Documentation)

Access the interactive API documentation at:

**URL:** http://localhost:8080/swagger-ui.html

The Swagger UI provides:
- Complete API reference with request/response examples
- Try-it-out functionality for testing endpoints
- Schema definitions for all data models
- Real-time API testing without writing code

### OpenAPI Specification

Raw OpenAPI 3.0 specification available at:

**URL:** http://localhost:8080/api-docs

Use this for:
- Generating client SDKs in various languages
- Importing into API testing tools (Postman, Insomnia)
- API contract validation

## Authentication

**Current Version:** No authentication required

The current API does not implement authentication. All endpoints are publicly accessible for development and demonstration purposes.

**Future Plans:**
- JWT-based authentication
- OAuth 2.0 support
- API key authentication for service-to-service calls

## Base URL

### Local Development
```
http://localhost:8080
```

### Production
```
https://api.yourdomain.com
```

All API endpoints are prefixed with `/api`.

## API Endpoints

### Products API

Manage product catalog operations.

#### Get Paginated Products

Retrieves a paginated list of products from the catalog.

**Endpoint:** `GET /api/products`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | integer | No | 1 | Page number (1-based) |

**Response:** `200 OK`

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

**cURL Example:**

```bash
curl -X GET "http://localhost:8080/api/products?page=1" \
  -H "Accept: application/json"
```

**JavaScript (Fetch) Example:**

```javascript
const response = await fetch('http://localhost:8080/api/products?page=1');
const products = await response.json();
console.log(products.data);
```

#### Get Product by Code

Retrieves a specific product by its unique code.

**Endpoint:** `GET /api/products/{code}`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `code` | string | Yes | Product code (e.g., "P100") |

**Response:** `200 OK`

```json
{
  "code": "P100",
  "name": "The Hunger Games",
  "description": "Winning will make you famous. Losing means certain death...",
  "imageUrl": "https://images.sivalabs.in/products/the-hunger-games.jpg",
  "price": 34.0
}
```

**Error Response:** `404 Not Found`

```json
{
  "status": 404,
  "message": "Product not found",
  "timestamp": "2025-10-18T10:30:00"
}
```

**cURL Example:**

```bash
curl -X GET "http://localhost:8080/api/products/P100" \
  -H "Accept: application/json"
```

**JavaScript Example:**

```javascript
try {
  const response = await fetch('http://localhost:8080/api/products/P100');
  if (!response.ok) throw new Error('Product not found');
  const product = await response.json();
  console.log(product);
} catch (error) {
  console.error('Error:', error);
}
```

---

### Cart API

Manage shopping cart operations. Cart state is maintained in HTTP session.

#### Get Cart

Retrieves the current shopping cart contents.

**Endpoint:** `GET /api/cart`

**Response:** `200 OK`

```json
{
  "items": [
    {
      "code": "P100",
      "name": "The Hunger Games",
      "price": 34.0,
      "quantity": 2,
      "subtotal": 68.0
    }
  ],
  "totalAmount": 68.0,
  "itemCount": 2
}
```

**cURL Example:**

```bash
curl -X GET "http://localhost:8080/api/cart" \
  -H "Accept: application/json" \
  -c cookies.txt -b cookies.txt
```

**JavaScript Example:**

```javascript
const response = await fetch('http://localhost:8080/api/cart', {
  credentials: 'include'  // Important: Include session cookies
});
const cart = await response.json();
```

#### Add Item to Cart

Adds a product to the shopping cart or updates quantity if already exists.

**Endpoint:** `POST /api/cart/items`

**Request Body:**

```json
{
  "code": "P100",
  "quantity": 2
}
```

**Validation Rules:**
- `code`: Required, product must exist
- `quantity`: Required, minimum value of 1

**Response:** `201 Created`

```json
{
  "items": [
    {
      "code": "P100",
      "name": "The Hunger Games",
      "price": 34.0,
      "quantity": 2,
      "subtotal": 68.0
    }
  ],
  "totalAmount": 68.0,
  "itemCount": 2
}
```

**Error Responses:**

- `400 Bad Request` - Invalid request data (quantity < 1)
- `404 Not Found` - Product not found

**cURL Example:**

```bash
curl -X POST "http://localhost:8080/api/cart/items" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -c cookies.txt -b cookies.txt \
  -d '{
    "code": "P100",
    "quantity": 2
  }'
```

**JavaScript Example:**

```javascript
const response = await fetch('http://localhost:8080/api/cart/items', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  credentials: 'include',
  body: JSON.stringify({
    code: 'P100',
    quantity: 2
  })
});

if (response.status === 201) {
  const cart = await response.json();
  console.log('Item added to cart:', cart);
}
```

#### Update Item Quantity

Updates the quantity of an item in the cart.

**Endpoint:** `PUT /api/cart/items/{code}`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `code` | string | Yes | Product code |

**Request Body:**

```json
{
  "quantity": 5
}
```

**Response:** `200 OK`

```json
{
  "items": [
    {
      "code": "P100",
      "name": "The Hunger Games",
      "price": 34.0,
      "quantity": 5,
      "subtotal": 170.0
    }
  ],
  "totalAmount": 170.0,
  "itemCount": 5
}
```

**Error Responses:**

- `400 Bad Request` - Invalid quantity (< 1)
- `404 Not Found` - Item not found in cart

**cURL Example:**

```bash
curl -X PUT "http://localhost:8080/api/cart/items/P100" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -c cookies.txt -b cookies.txt \
  -d '{"quantity": 5}'
```

#### Clear Cart

Removes all items from the shopping cart.

**Endpoint:** `DELETE /api/cart`

**Response:** `204 No Content`

**cURL Example:**

```bash
curl -X DELETE "http://localhost:8080/api/cart" \
  -c cookies.txt -b cookies.txt
```

**JavaScript Example:**

```javascript
await fetch('http://localhost:8080/api/cart', {
  method: 'DELETE',
  credentials: 'include'
});
console.log('Cart cleared');
```

---

### Orders API

Manage order operations. Orders are processed via gRPC backend service.

#### Create Order

Creates a new order from the provided order details.

**Endpoint:** `POST /api/orders`

**Request Body:**

```json
{
  "customer": {
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+1-555-123-4567"
  },
  "deliveryAddress": "742 Evergreen Terrace, Springfield",
  "item": {
    "code": "P100",
    "name": "The Hunger Games",
    "price": 34.0,
    "quantity": 2
  }
}
```

**Validation Rules:**
- `customer.name`: Required
- `customer.email`: Required, valid email format
- `customer.phone`: Required
- `deliveryAddress`: Required
- `item.code`: Required
- `item.name`: Required
- `item.price`: Required
- `item.quantity`: Required, minimum value of 1

**Response:** `201 Created`

```json
{
  "orderNumber": "ORD-2025-001234"
}
```

**Error Responses:**

- `400 Bad Request` - Invalid order data
- `503 Service Unavailable` - Orders service unavailable

**cURL Example:**

```bash
curl -X POST "http://localhost:8080/api/orders" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "customer": {
      "name": "John Doe",
      "email": "john.doe@example.com",
      "phone": "+1-555-123-4567"
    },
    "deliveryAddress": "742 Evergreen Terrace, Springfield",
    "item": {
      "code": "P100",
      "name": "The Hunger Games",
      "price": 34.0,
      "quantity": 2
    }
  }'
```

**JavaScript Example:**

```javascript
const orderRequest = {
  customer: {
    name: 'John Doe',
    email: 'john.doe@example.com',
    phone: '+1-555-123-4567'
  },
  deliveryAddress: '742 Evergreen Terrace, Springfield',
  item: {
    code: 'P100',
    name: 'The Hunger Games',
    price: 34.0,
    quantity: 2
  }
};

try {
  const response = await fetch('http://localhost:8080/api/orders', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(orderRequest)
  });

  if (response.status === 201) {
    const result = await response.json();
    console.log('Order created:', result.orderNumber);
  } else if (response.status === 503) {
    console.error('Service temporarily unavailable');
  }
} catch (error) {
  console.error('Failed to create order:', error);
}
```

#### List Orders

Retrieves a list of all orders.

**Endpoint:** `GET /api/orders`

**Response:** `200 OK`

```json
[
  {
    "orderNumber": "ORD-2025-001234",
    "status": "NEW",
    "customer": {
      "name": "John Doe",
      "email": "john.doe@example.com",
      "phone": "+1-555-123-4567"
    }
  },
  {
    "orderNumber": "ORD-2025-001235",
    "status": "DELIVERED",
    "customer": {
      "name": "Jane Smith",
      "email": "jane.smith@example.com",
      "phone": "+1-555-987-6543"
    }
  }
]
```

**Error Response:**

- `503 Service Unavailable` - Orders service unavailable

**cURL Example:**

```bash
curl -X GET "http://localhost:8080/api/orders" \
  -H "Accept: application/json"
```

#### Get Order Details

Retrieves detailed information about a specific order.

**Endpoint:** `GET /api/orders/{orderNumber}`

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orderNumber` | string | Yes | Order number (e.g., "ORD-2025-001234") |

**Response:** `200 OK`

```json
{
  "orderNumber": "ORD-2025-001234",
  "item": {
    "code": "P100",
    "name": "The Hunger Games",
    "price": 34.0,
    "quantity": 2
  },
  "customer": {
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+1-555-123-4567"
  },
  "deliveryAddress": "742 Evergreen Terrace, Springfield",
  "status": "NEW",
  "createdAt": "2025-10-18T10:30:00",
  "totalAmount": 68.0
}
```

**Error Responses:**

- `404 Not Found` - Order not found
- `503 Service Unavailable` - Orders service unavailable

**cURL Example:**

```bash
curl -X GET "http://localhost:8080/api/orders/ORD-2025-001234" \
  -H "Accept: application/json"
```

**JavaScript Example:**

```javascript
const orderNumber = 'ORD-2025-001234';
const response = await fetch(`http://localhost:8080/api/orders/${orderNumber}`);

if (response.ok) {
  const order = await response.json();
  console.log('Order details:', order);
} else if (response.status === 404) {
  console.error('Order not found');
} else if (response.status === 503) {
  console.error('Service unavailable');
}
```

---

## Data Models

### ProductDto

Product information.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `code` | string | Yes | Unique product code |
| `name` | string | Yes | Product name |
| `description` | string | No | Product description |
| `imageUrl` | string | No | Product image URL |
| `price` | number | Yes | Product price |

### CartDto

Shopping cart with items and total.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `items` | CartItemDto[] | Yes | Cart items |
| `totalAmount` | number | Yes | Total amount |
| `itemCount` | integer | Yes | Number of items in cart (total quantity) |

### CartItemDto

Shopping cart line item.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `code` | string | Yes | Product code |
| `name` | string | Yes | Product name |
| `price` | number | Yes | Unit price |
| `quantity` | integer | Yes | Quantity (min: 1) |
| `subtotal` | number | Yes | Subtotal (price × quantity) |

### CreateOrderRequest

Request to create a new order.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `customer` | Customer | Yes | Customer information |
| `deliveryAddress` | string | Yes | Delivery address |
| `item` | OrderItem | Yes | Order item |

### Customer

Customer information for order.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Customer full name |
| `email` | string | Yes | Customer email address |
| `phone` | string | Yes | Customer phone number |

### OrderItem

Order line item representing a product in the order.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `code` | string | Yes | Product code |
| `name` | string | Yes | Product name |
| `price` | number | Yes | Product price |
| `quantity` | integer | Yes | Order quantity (min: 1) |

### OrderDto

Complete order details.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderNumber` | string | Yes | Unique order number |
| `item` | OrderItem | Yes | Order item |
| `customer` | Customer | Yes | Customer information |
| `deliveryAddress` | string | Yes | Delivery address |
| `status` | OrderStatus | Yes | Order status |
| `createdAt` | datetime | Yes | Order creation timestamp |
| `totalAmount` | number | Yes | Total order amount (read-only) |

### OrderStatus

Order status enumeration.

**Values:**
- `NEW` - Order created
- `IN_PROCESS` - Order being processed
- `DELIVERED` - Order delivered
- `CANCELLED` - Order cancelled
- `ERROR` - Order processing error

### PagedResult

Paginated response containing data and pagination metadata.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `data` | array | Yes | List of items for the current page |
| `totalElements` | integer | Yes | Total number of elements across all pages |
| `pageNumber` | integer | Yes | Current page number (1-based) |
| `totalPages` | integer | Yes | Total number of pages |
| `isFirst` | boolean | Yes | Whether this is the first page |
| `isLast` | boolean | Yes | Whether this is the last page |
| `hasNext` | boolean | Yes | Whether there is a next page available |
| `hasPrevious` | boolean | Yes | Whether there is a previous page available |

---

## Error Handling

### Error Response Format

All error responses follow a consistent format:

```json
{
  "status": 404,
  "message": "Resource not found",
  "timestamp": "2025-10-18T10:30:00"
}
```

### HTTP Status Codes

| Status Code | Description | When It Occurs |
|-------------|-------------|----------------|
| `200 OK` | Success | Request processed successfully |
| `201 Created` | Created | Resource created successfully |
| `204 No Content` | Success (no content) | Request succeeded with no response body |
| `400 Bad Request` | Client error | Invalid request data or validation failure |
| `404 Not Found` | Not found | Resource does not exist |
| `500 Internal Server Error` | Server error | Unexpected server error |
| `503 Service Unavailable` | Service unavailable | Backend service (e.g., gRPC) unavailable |
| `504 Gateway Timeout` | Timeout | Request to backend service timed out |

### Common Error Scenarios

#### Validation Errors (400)

Occurs when request data fails validation:

```javascript
// Example: Invalid quantity
{
  "status": 400,
  "message": "Quantity must be greater than 0",
  "timestamp": "2025-10-18T10:30:00"
}
```

#### Resource Not Found (404)

Occurs when requested resource doesn't exist:

```javascript
// Example: Product not found
{
  "status": 404,
  "message": "Product not found",
  "timestamp": "2025-10-18T10:30:00"
}
```

#### Service Unavailable (503)

Occurs when backend service (gRPC) is unavailable:

```javascript
{
  "status": 503,
  "message": "Orders service unavailable. Please try again later.",
  "timestamp": "2025-10-18T10:30:00"
}
```

**Recommended Retry Strategy:**

For 503 errors, implement exponential backoff retry:

```javascript
async function createOrderWithRetry(orderRequest, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const response = await fetch('/api/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(orderRequest)
      });

      if (response.status === 503 && i < maxRetries - 1) {
        // Wait before retry: 1s, 2s, 4s
        await new Promise(resolve => setTimeout(resolve, 1000 * Math.pow(2, i)));
        continue;
      }

      return response;
    } catch (error) {
      if (i === maxRetries - 1) throw error;
    }
  }
}
```

---

## TypeScript SDK

A type-safe TypeScript SDK is available for frontend applications.

### Installation

**Using local path (development):**

```json
{
  "dependencies": {
    "bookstore-api-client": "file:../spring-modular-monolith/frontend-sdk",
    "axios": "^1.6.0"
  }
}
```

### Quick Start

```typescript
import { Configuration, ProductsApi, CartApi, OrdersApi } from 'bookstore-api-client';

// Create configuration
const config = new Configuration({
  basePath: 'http://localhost:8080'
});

// Create API clients
const productsApi = new ProductsApi(config);
const cartApi = new CartApi(config);
const ordersApi = new OrdersApi(config);

// Use with full type safety
const response = await productsApi.getProducts(1);
const products = response.data;
```

### Documentation

Complete SDK usage examples and documentation available at:
- **SDK Usage Guide:** `frontend-sdk/USAGE-EXAMPLES.md`
- **Generated Docs:** `frontend-sdk/docs/`

---

## Code Examples

### Complete Shopping Flow

**JavaScript (Vanilla):**

```javascript
// 1. Browse products
const productsResponse = await fetch('http://localhost:8080/api/products?page=1');
const productsPage = await productsResponse.json();
console.log('Products:', productsPage.data);

// 2. Get product details
const productResponse = await fetch('http://localhost:8080/api/products/P100');
const product = await productResponse.json();
console.log('Product:', product);

// 3. Add to cart
const addToCartResponse = await fetch('http://localhost:8080/api/cart/items', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({ code: 'P100', quantity: 2 })
});
const cart = await addToCartResponse.json();
console.log('Cart:', cart);

// 4. View cart
const cartResponse = await fetch('http://localhost:8080/api/cart', {
  credentials: 'include'
});
const currentCart = await cartResponse.json();
console.log('Current cart:', currentCart);

// 5. Create order
const orderResponse = await fetch('http://localhost:8080/api/orders', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    customer: {
      name: 'John Doe',
      email: 'john@example.com',
      phone: '+1-555-123-4567'
    },
    deliveryAddress: '742 Evergreen Terrace',
    item: {
      code: 'P100',
      name: product.name,
      price: product.price,
      quantity: 2
    }
  })
});
const order = await orderResponse.json();
console.log('Order created:', order.orderNumber);

// 6. View order details
const orderDetailsResponse = await fetch(
  `http://localhost:8080/api/orders/${order.orderNumber}`
);
const orderDetails = await orderDetailsResponse.json();
console.log('Order details:', orderDetails);
```

**TypeScript SDK:**

```typescript
import { productsApi, cartApi, ordersApi } from './api/client';

async function completePurchaseFlow() {
  // 1. Browse products
  const productsPage = await productsApi.getProducts(1);
  console.log('Products:', productsPage.data);

  // 2. Get product details
  const product = await productsApi.getProductByCode('P100');
  console.log('Product:', product.data);

  // 3. Add to cart
  const cart = await cartApi.addItem({ code: 'P100', quantity: 2 });
  console.log('Cart:', cart.data);

  // 4. Create order
  const orderResponse = await ordersApi.createOrder({
    customer: {
      name: 'John Doe',
      email: 'john@example.com',
      phone: '+1-555-123-4567'
    },
    deliveryAddress: '742 Evergreen Terrace',
    item: {
      code: 'P100',
      name: product.data.name!,
      price: product.data.price!,
      quantity: 2
    }
  });
  console.log('Order created:', orderResponse.data.orderNumber);

  // 5. View order details
  const orderDetails = await ordersApi.getOrder(orderResponse.data.orderNumber!);
  console.log('Order details:', orderDetails.data);
}
```

---

## Rate Limiting

**Current Version:** No rate limiting implemented

Future versions may implement rate limiting. Recommended client-side practices:
- Implement request throttling
- Cache frequently accessed data
- Use appropriate retry strategies with exponential backoff

---

## Versioning

**Current Version:** 1.0.0

The API follows semantic versioning (SemVer). Breaking changes will be introduced in major version updates.

**Future Plans:**
- API versioning via URL path (`/api/v2/...`)
- Backward compatibility support for at least one major version

---

## Support

For questions, issues, or feature requests:
- **GitHub Issues:** [Repository Issues](https://github.com/yourusername/spring-modular-monolith/issues)
- **Documentation:** See main [README.md](README.md) for project documentation
- **Interactive API Docs:** http://localhost:8080/swagger-ui.html
