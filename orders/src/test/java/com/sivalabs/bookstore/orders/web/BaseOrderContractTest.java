package com.sivalabs.bookstore.orders.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.OrdersApi;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderRestController.class)
@Import(OrdersExceptionHandler.class)
abstract class BaseOrderContractTest {

    static final String ORDER_NUMBER = "OR-123456";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    OrdersApi ordersApi;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        Customer customer = new Customer("John Doe", "john@example.com", "123-456-7890");
        OrderItem orderItem = new OrderItem("P100", "Domain-Driven Design", new BigDecimal("49.99"), 1);

        OrderDto orderDto = new OrderDto(
                ORDER_NUMBER,
                orderItem,
                customer,
                "221B Baker Street",
                OrderStatus.NEW,
                LocalDateTime.parse("2024-01-20T10:15:00"));

        OrderView orderView = new OrderView(ORDER_NUMBER, OrderStatus.NEW, customer);

        given(ordersApi.createOrder(any(CreateOrderRequest.class))).willReturn(new CreateOrderResponse(ORDER_NUMBER));
        given(ordersApi.findOrder(anyString())).willReturn(Optional.empty());
        given(ordersApi.findOrder(ORDER_NUMBER)).willReturn(Optional.of(orderDto));
        given(ordersApi.findOrders()).willReturn(List.of(orderView));
    }

    @AfterEach
    void tearDown() {
        RestAssuredMockMvc.reset();
    }
}
