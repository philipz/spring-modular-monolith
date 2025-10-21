package com.sivalabs.bookstore.orders.web.cart;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.bookstore.catalog.api.ProductApi;
import com.sivalabs.bookstore.catalog.api.ProductDto;
import com.sivalabs.bookstore.orders.web.cart.dto.AddToCartRequest;
import com.sivalabs.bookstore.orders.web.cart.dto.UpdateQuantityRequest;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartRestController.class)
class CartRestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductApi productApi;

    @Test
    void shouldAddItemToCart() throws Exception {
        ProductDto product = new ProductDto(
                "P100", "Test Product", "A test product description", "test.jpg", new BigDecimal("29.99"));
        when(productApi.getByCode("P100")).thenReturn(Optional.of(product));

        AddToCartRequest request = new AddToCartRequest("P100", 2);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/cart/items")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].code", is("P100")))
                .andExpect(jsonPath("$.items[0].name", is("Test Product")))
                .andExpect(jsonPath("$.items[0].price", is(29.99)))
                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.items[0].subtotal", is(59.98)))
                .andExpect(jsonPath("$.totalAmount", is(59.98)))
                .andExpect(jsonPath("$.itemCount", is(2)));
    }

    @Test
    void shouldReturnNotFoundWhenAddingNonExistentProduct() throws Exception {
        when(productApi.getByCode(anyString())).thenReturn(Optional.empty());

        AddToCartRequest request = new AddToCartRequest("INVALID", 1);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/cart/items")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestForInvalidAddToCartRequest() throws Exception {
        AddToCartRequest request = new AddToCartRequest("P100", 0);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/cart/items")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateItemQuantity() throws Exception {
        ProductDto product = new ProductDto(
                "P100", "Test Product", "A test product description", "test.jpg", new BigDecimal("29.99"));
        when(productApi.getByCode("P100")).thenReturn(Optional.of(product));

        MockHttpSession session = new MockHttpSession();

        AddToCartRequest addRequest = new AddToCartRequest("P100", 2);
        mockMvc.perform(post("/api/cart/items")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)));

        UpdateQuantityRequest updateRequest = new UpdateQuantityRequest(5);
        mockMvc.perform(put("/api/cart/items/{code}", "P100")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity", is(5)))
                .andExpect(jsonPath("$.items[0].subtotal", is(149.95)))
                .andExpect(jsonPath("$.totalAmount", is(149.95)));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentItem() throws Exception {
        MockHttpSession session = new MockHttpSession();

        UpdateQuantityRequest request = new UpdateQuantityRequest(3);
        mockMvc.perform(put("/api/cart/items/{code}", "P100")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestForInvalidQuantityUpdate() throws Exception {
        ProductDto product = new ProductDto(
                "P100", "Test Product", "A test product description", "test.jpg", new BigDecimal("29.99"));
        when(productApi.getByCode("P100")).thenReturn(Optional.of(product));

        MockHttpSession session = new MockHttpSession();

        AddToCartRequest addRequest = new AddToCartRequest("P100", 2);
        mockMvc.perform(post("/api/cart/items")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)));

        UpdateQuantityRequest updateRequest = new UpdateQuantityRequest(0);
        mockMvc.perform(put("/api/cart/items/{code}", "P100")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetCart() throws Exception {
        ProductDto product = new ProductDto(
                "P100", "Test Product", "A test product description", "test.jpg", new BigDecimal("29.99"));
        when(productApi.getByCode("P100")).thenReturn(Optional.of(product));

        MockHttpSession session = new MockHttpSession();

        AddToCartRequest addRequest = new AddToCartRequest("P100", 2);
        mockMvc.perform(post("/api/cart/items")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)));

        mockMvc.perform(get("/api/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code", is("P100")))
                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.totalAmount", is(59.98)))
                .andExpect(jsonPath("$.itemCount", is(2)));
    }

    @Test
    void shouldGetEmptyCart() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/api/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalAmount", is(0)))
                .andExpect(jsonPath("$.itemCount", is(0)));
    }

    @Test
    void shouldClearCart() throws Exception {
        ProductDto product = new ProductDto(
                "P100", "Test Product", "A test product description", "test.jpg", new BigDecimal("29.99"));
        when(productApi.getByCode("P100")).thenReturn(Optional.of(product));

        MockHttpSession session = new MockHttpSession();

        AddToCartRequest addRequest = new AddToCartRequest("P100", 2);
        mockMvc.perform(post("/api/cart/items")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)));

        mockMvc.perform(delete("/api/cart").session(session)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalAmount", is(0)))
                .andExpect(jsonPath("$.itemCount", is(0)));
    }

    @Test
    void shouldHandleMultipleSessionsIndependently() throws Exception {
        ProductDto product = new ProductDto(
                "P100", "Test Product", "A test product description", "test.jpg", new BigDecimal("29.99"));
        when(productApi.getByCode("P100")).thenReturn(Optional.of(product));

        MockHttpSession session1 = new MockHttpSession();
        MockHttpSession session2 = new MockHttpSession();

        AddToCartRequest request = new AddToCartRequest("P100", 2);

        mockMvc.perform(post("/api/cart/items")
                .session(session1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/cart").session(session1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount", is(2)));

        mockMvc.perform(get("/api/cart").session(session2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount", is(0)));
    }
}
