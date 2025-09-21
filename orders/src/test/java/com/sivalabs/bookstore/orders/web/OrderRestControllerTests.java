package com.sivalabs.bookstore.orders.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.OrdersApi;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import com.sivalabs.bookstore.orders.infrastructure.catalog.CatalogServiceException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderRestController.class)
@Import(OrdersExceptionHandler.class)
class OrderRestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrdersApi ordersApi;

    @Test
    @DisplayName("Should create order successfully")
    void shouldCreateOrderSuccessfully() throws Exception {
        CreateOrderRequest request = createOrderRequest();
        CreateOrderResponse response = new CreateOrderResponse("BK-123456");
        given(ordersApi.createOrder(any(CreateOrderRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("BK-123456"));

        ArgumentCaptor<CreateOrderRequest> captor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        org.mockito.Mockito.verify(ordersApi).createOrder(captor.capture());
        CreateOrderRequest captured = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(captured.customer().email()).isEqualTo("buyer@example.com");
        org.assertj.core.api.Assertions.assertThat(captured.item().code()).isEqualTo("P100");
    }

    @Test
    @DisplayName("Should return order details when found")
    void shouldReturnOrderDetailsWhenFound() throws Exception {
        OrderDto order = new OrderDto(
                "BK-123456",
                new OrderItem("P100", "Domain-Driven Design", new BigDecimal("49.99"), 1),
                new Customer("Buyer", "buyer@example.com", "999-999-9999"),
                "221B Baker Street",
                OrderStatus.NEW,
                LocalDateTime.parse("2024-01-20T10:15:00"));
        given(ordersApi.findOrder(eq("BK-123456"))).willReturn(Optional.of(order));

        mockMvc.perform(get("/api/orders/{orderNumber}", "BK-123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("BK-123456"))
                .andExpect(jsonPath("$.item.name").value("Domain-Driven Design"))
                .andExpect(jsonPath("$.customer.email").value("buyer@example.com"));
    }

    @Test
    @DisplayName("Should surface catalog service outage as 503 problem detail")
    void shouldReturnServiceUnavailableWhenCatalogUnavailable() throws Exception {
        CreateOrderRequest request = createOrderRequest();
        given(ordersApi.createOrder(any(CreateOrderRequest.class)))
                .willThrow(new CatalogServiceException("Unable to fetch product details from catalog service", null));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Catalog Service Unavailable"))
                .andExpect(jsonPath("$.detail").value("Unable to fetch product details from catalog service"));
    }

    @Test
    @DisplayName("Should return not found problem detail when order missing")
    void shouldReturnNotFoundProblemDetailWhenOrderMissing() throws Exception {
        given(ordersApi.findOrder(eq("missing-order"))).willReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/{orderNumber}", "missing-order"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order Not Found"))
                .andExpect(jsonPath("$.detail").value("Order not found with orderNumber: missing-order"));
    }

    @Test
    @DisplayName("Should return list of orders")
    void shouldReturnListOfOrders() throws Exception {
        List<OrderView> orders = List.of(new OrderView(
                "BK-123456", OrderStatus.IN_PROCESS, new Customer("Buyer", "buyer@example.com", "999-999-9999")));
        given(ordersApi.findOrders()).willReturn(orders);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("BK-123456"))
                .andExpect(jsonPath("$[0].status").value("IN_PROCESS"));
    }

    private CreateOrderRequest createOrderRequest() {
        Customer customer = new Customer("Buyer", "buyer@example.com", "999-999-9999");
        OrderItem item = new OrderItem("P100", "Domain-Driven Design", new BigDecimal("49.99"), 1);
        return new CreateOrderRequest(customer, "221B Baker Street", item);
    }
}
