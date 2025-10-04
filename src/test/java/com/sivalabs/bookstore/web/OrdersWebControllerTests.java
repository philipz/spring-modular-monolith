package com.sivalabs.bookstore.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sivalabs.bookstore.orders.InvalidOrderException;
import com.sivalabs.bookstore.orders.OrderNotFoundException;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.OrdersRemoteClient;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrdersWebController.class)
class OrdersWebControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrdersRemoteClient ordersClient;

    private MockHttpSession sessionWithCart;

    @BeforeEach
    void setUpCart() {
        sessionWithCart = new MockHttpSession();
        Cart cart = new Cart();
        cart.setItem(new Cart.LineItem("P100", "Test Product", new BigDecimal("19.99"), 1));
        CartUtil.setCart(sessionWithCart, cart);
    }

    @Test
    void createOrderRedirectsToDetailsOnSuccess() throws Exception {
        when(ordersClient.createOrder(any())).thenReturn(new CreateOrderResponse("ORD-123"));

        mockMvc.perform(post("/orders")
                        .session(sessionWithCart)
                        .param("customer.name", "Alice")
                        .param("customer.email", "alice@example.com")
                        .param("customer.phone", "+1234567890")
                        .param("deliveryAddress", "221B Baker Street"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/ORD-123"));
    }

    @Test
    void createOrderReturns400ViewWhenValidationFailsInGrpc() throws Exception {
        when(ordersClient.createOrder(any())).thenThrow(new InvalidOrderException("Quantity must be greater than 0"));

        mockMvc.perform(post("/orders")
                        .session(sessionWithCart)
                        .param("customer.name", "Bob")
                        .param("customer.email", "bob@example.com")
                        .param("customer.phone", "+1987654321")
                        .param("deliveryAddress", "742 Evergreen Terrace"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"))
                .andExpect(model().attribute("message", "Quantity must be greater than 0"));
    }

    @Test
    void showOrdersReturnsPartialForHtmxRequests() throws Exception {
        when(ordersClient.listOrders())
                .thenReturn(List.of(new OrderView("ORD-1", OrderStatus.NEW, new Customer("CX", "cx@test.com", "+1"))));

        mockMvc.perform(get("/orders").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("partials/orders"))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    void showOrderDetailsReturns404WhenOrderMissing() throws Exception {
        when(ordersClient.getOrder("ORD-404")).thenThrow(new OrderNotFoundException("Order not found: ORD-404"));

        mockMvc.perform(get("/orders/ORD-404"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("message", "Order not found: ORD-404"));
    }
}
