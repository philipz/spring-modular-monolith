package com.sivalabs.bookstore.orders.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.bookstore.orders.web.dto.AddToCartRequest;
import com.sivalabs.bookstore.orders.web.dto.UpdateQuantityRequest;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartRestController.class)
@Import(OrdersExceptionHandler.class)
class CartRestControllerTests {

    private static final String PRODUCT_CODE = "P100";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductApiAdapter productApiAdapter;

    @Test
    @DisplayName("Should return empty cart for new session")
    void shouldReturnEmptyCartForNewSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/api/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item").doesNotExist())
                .andExpect(jsonPath("$.data.totalAmount").value(0));
    }

    @Test
    @DisplayName("Should add item to cart")
    void shouldAddItemToCart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        given(productApiAdapter.getByCode(PRODUCT_CODE))
                .willReturn(Optional.of(new ProductApiAdapter.ProductDto(
                        PRODUCT_CODE, "Domain-Driven Design", "Classic DDD book", "", new BigDecimal("49.99"))));

        AddToCartRequest request = new AddToCartRequest(PRODUCT_CODE);

        mockMvc.perform(post("/api/cart/items")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.item.code").value(PRODUCT_CODE))
                .andExpect(jsonPath("$.data.totalAmount").value(49.99));
    }

    @Test
    @DisplayName("Should update quantity and remove item when quantity zero")
    void shouldUpdateQuantityAndRemoveItemWhenZero() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Cart cart = new Cart();
        cart.setItem(new Cart.LineItem(PRODUCT_CODE, "DDD", new BigDecimal("10.00"), 2));
        CartUtil.setCart(session, cart);

        UpdateQuantityRequest request = new UpdateQuantityRequest(0);

        mockMvc.perform(put("/api/cart/items/{code}", PRODUCT_CODE)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item").doesNotExist())
                .andExpect(jsonPath("$.data.totalAmount").value(0));
    }

    @Test
    @DisplayName("Should return problem detail when product mismatch")
    void shouldReturnProblemDetailWhenProductMismatch() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Cart cart = new Cart();
        cart.setItem(new Cart.LineItem(PRODUCT_CODE, "DDD", new BigDecimal("10.00"), 1));
        CartUtil.setCart(session, cart);

        UpdateQuantityRequest request = new UpdateQuantityRequest(1);

        mockMvc.perform(put("/api/cart/items/{code}", "OTHER")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Cart Operation"))
                .andExpect(jsonPath("$.detail").value("Cart contains product P100 but request targeted OTHER"));
    }

    @Test
    @DisplayName("Should return problem detail when product unavailable")
    void shouldReturnProblemDetailWhenProductUnavailable() throws Exception {
        MockHttpSession session = new MockHttpSession();
        given(productApiAdapter.getByCode(anyString())).willReturn(Optional.empty());

        AddToCartRequest request = new AddToCartRequest("UNKNOWN");

        mockMvc.perform(post("/api/cart/items")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Cart Operation"))
                .andExpect(jsonPath("$.detail").value("Product not available for code: UNKNOWN"));
    }
}
