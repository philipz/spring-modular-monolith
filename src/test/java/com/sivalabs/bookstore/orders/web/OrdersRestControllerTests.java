package com.sivalabs.bookstore.orders.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.bookstore.common.models.PagedResult;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.OrdersRemoteClient;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for OrdersRestController using @WebMvcTest.
 * Tests all orders REST API endpoints with mocked OrdersRemoteClient including gRPC error scenarios.
 */
@WebMvcTest(OrdersRestController.class)
class OrdersRestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrdersRemoteClient ordersRemoteClient;

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        // Given: Valid order request
        Customer customer = new Customer("John Doe", "john.doe@example.com", "+1-555-123-4567");
        OrderItem item = new OrderItem("P100", "The Hunger Games", new BigDecimal("34.0"), 2);
        CreateOrderRequest request = new CreateOrderRequest(customer, "742 Evergreen Terrace, Springfield", item);

        CreateOrderResponse response = new CreateOrderResponse("ORD-2025-001234");
        when(ordersRemoteClient.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        // When: Creating order
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Order created successfully
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber", is("ORD-2025-001234")));
    }

    @Test
    void shouldReturnBadRequestForInvalidOrderData() throws Exception {
        // Given: Invalid order request (gRPC returns INVALID_ARGUMENT)
        Customer customer = new Customer("John Doe", "john.doe@example.com", "+1-555-123-4567");
        OrderItem item = new OrderItem("P100", "The Hunger Games", new BigDecimal("34.0"), 2);
        CreateOrderRequest request = new CreateOrderRequest(customer, "742 Evergreen Terrace, Springfield", item);

        when(ordersRemoteClient.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Invalid order data")));

        // When: Creating order with invalid data
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Returns 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    void shouldReturnServiceUnavailableWhenGrpcUnavailable() throws Exception {
        // Given: gRPC service unavailable
        Customer customer = new Customer("John Doe", "john.doe@example.com", "+1-555-123-4567");
        OrderItem item = new OrderItem("P100", "The Hunger Games", new BigDecimal("34.0"), 2);
        CreateOrderRequest request = new CreateOrderRequest(customer, "742 Evergreen Terrace, Springfield", item);

        when(ordersRemoteClient.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        // When: Creating order when service unavailable
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Returns 503 Service Unavailable
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnGatewayTimeoutWhenDeadlineExceeded() throws Exception {
        // Given: gRPC deadline exceeded
        Customer customer = new Customer("John Doe", "john.doe@example.com", "+1-555-123-4567");
        OrderItem item = new OrderItem("P100", "The Hunger Games", new BigDecimal("34.0"), 2);
        CreateOrderRequest request = new CreateOrderRequest(customer, "742 Evergreen Terrace, Springfield", item);

        when(ordersRemoteClient.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.DEADLINE_EXCEEDED));

        // When: Creating order when deadline exceeded
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Returns 504 Gateway Timeout
                .andExpect(status().isGatewayTimeout());
    }

    @Test
    void shouldReturnInternalServerErrorForUnknownGrpcError() throws Exception {
        // Given: Unknown gRPC error
        Customer customer = new Customer("John Doe", "john.doe@example.com", "+1-555-123-4567");
        OrderItem item = new OrderItem("P100", "The Hunger Games", new BigDecimal("34.0"), 2);
        CreateOrderRequest request = new CreateOrderRequest(customer, "742 Evergreen Terrace, Springfield", item);

        when(ordersRemoteClient.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.INTERNAL));

        // When: Creating order with unknown error
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Returns 500 Internal Server Error
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldListOrdersSuccessfully() throws Exception {
        // Given: Orders exist
        Customer customer = new Customer("John Doe", "john.doe@example.com", "+1-555-123-4567");
        OrderView order1 = new OrderView("ORD-2025-001234", OrderStatus.NEW, customer);
        OrderView order2 = new OrderView("ORD-2025-001235", OrderStatus.DELIVERED, customer);
        PagedResult<OrderView> orders =
                new PagedResult<>(java.util.List.of(order1, order2), 2, 1, 1, true, true, false, false);

        when(ordersRemoteClient.listOrders(anyInt(), anyInt())).thenReturn(orders);

        // When: Listing orders
        mockMvc.perform(get("/api/orders").param("page", "1").param("pageSize", "20"))
                // Then: Returns orders successfully
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].orderNumber", is("ORD-2025-001234")))
                .andExpect(jsonPath("$.data[0].status", is("NEW")))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.pageNumber", is(1)));
    }

    @Test
    void shouldReturnServiceUnavailableWhenListOrdersFails() throws Exception {
        // Given: gRPC service unavailable
        when(ordersRemoteClient.listOrders(anyInt(), anyInt()))
                .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        // When: Listing orders when service unavailable
        mockMvc.perform(get("/api/orders"))
                // Then: Returns 503 Service Unavailable
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldGetOrderSuccessfully() throws Exception {
        // Given: Order exists
        Customer customer = new Customer("John Doe", "john.doe@example.com", "+1-555-123-4567");
        OrderItem item = new OrderItem("P100", "The Hunger Games", new BigDecimal("34.0"), 2);
        OrderDto order = new OrderDto(
                "ORD-2025-001234", item, customer, "742 Evergreen Terrace", OrderStatus.NEW, LocalDateTime.now());

        when(ordersRemoteClient.getOrder("ORD-2025-001234")).thenReturn(order);

        // When: Getting order by number
        mockMvc.perform(get("/api/orders/ORD-2025-001234"))
                // Then: Returns order successfully
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber", is("ORD-2025-001234")))
                .andExpect(jsonPath("$.status", is("NEW")))
                .andExpect(jsonPath("$.customer.name", is("John Doe")))
                .andExpect(jsonPath("$.item.code", is("P100")))
                .andExpect(jsonPath("$.item.quantity", is(2)))
                .andExpect(jsonPath("$.deliveryAddress", is("742 Evergreen Terrace")));
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        // Given: Order does not exist (gRPC returns NOT_FOUND)
        when(ordersRemoteClient.getOrder(anyString()))
                .thenThrow(new StatusRuntimeException(Status.NOT_FOUND.withDescription("Order not found")));

        // When: Getting non-existent order
        mockMvc.perform(get("/api/orders/INVALID-ORDER"))
                // Then: Returns 404 Not Found
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnServiceUnavailableWhenGetOrderFails() throws Exception {
        // Given: gRPC service unavailable
        when(ordersRemoteClient.getOrder(anyString())).thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        // When: Getting order when service unavailable
        mockMvc.perform(get("/api/orders/ORD-2025-001234"))
                // Then: Returns 503 Service Unavailable
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnBadRequestForValidationErrors() throws Exception {
        // Given: Invalid request (missing required fields)
        String invalidJson = "{\"customer\": null, \"deliveryAddress\": \"\", \"item\": null}";

        // When: Creating order with invalid data
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                // Then: Returns 400 Bad Request
                .andExpect(status().isBadRequest());
    }
}
