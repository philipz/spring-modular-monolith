# OpenAPI Documentation for Orders Service

## Overview

This service provides comprehensive OpenAPI/Swagger documentation for all REST endpoints.

## OpenAPI Endpoints

Once the service is running, the following endpoints are available:

### OpenAPI JSON Specification
- **URL**: `http://localhost:8091/api-docs`
- **Format**: JSON
- **Description**: Raw OpenAPI 3.0 specification document

### Swagger UI
- **URL**: `http://localhost:8091/swagger-ui.html`
- **Description**: Interactive web interface for exploring and testing the API

## API Endpoints Documented

### Orders API
- `POST /api/orders` - Create a new order
- `GET /api/orders` - Get all orders
- `GET /api/orders/{orderNumber}` - Get order by order number

## Schema Documentation

All request/response DTOs are fully documented with:
- Field descriptions and examples
- Validation constraints
- Required field indicators
- Data types and formats

### Key Schemas
- `CreateOrderRequest` - Request to create new orders
- `CreateOrderResponse` - Response after order creation
- `OrderDto` - Complete order information
- `OrderView` - Simplified order view for listings
- `Customer` - Customer information
- `OrderItem` - Order item details
- `OrderStatus` - Order status enumeration

## Configuration

The OpenAPI documentation is configured with:
- Service title: "Orders Service API"
- Description: "Orders microservice extracted from the bookstore modular monolith"
- Version: "1.0.0"
- Development server: "http://localhost:8091"
- Sorted operations and tags alphabetically
- Actuator endpoints excluded from documentation

## Usage

1. Start the orders service
2. Navigate to `http://localhost:8091/swagger-ui.html` to explore the API interactively
3. Use `http://localhost:8091/api-docs` to access the raw OpenAPI specification

The OpenAPI specification can be used for:
- API contract validation
- Code generation for clients
- Integration with API gateways
- Contract testing frameworks