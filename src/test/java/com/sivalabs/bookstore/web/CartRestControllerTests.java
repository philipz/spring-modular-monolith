package com.sivalabs.bookstore.web;

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
import com.sivalabs.bookstore.web.dto.AddToCartRequest;
import com.sivalabs.bookstore.web.dto.UpdateQuantityRequest;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for CartRestController using @WebMvcTest.
 * Tests all cart REST API endpoints with mocked dependencies.
 */
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
        // Given: A product exists
        ProductDto product = new ProductDto(
                "P100", "Test Product", "A test product description", "test.jpg", new BigDecimal("29.99"));
        when(productApi.getByCode("P100")).thenReturn(Optional.of(product));

        AddToCartRequest request = new AddToCartRequest("P100", 2);
        MockHttpSession session = new MockHttpSession();

        // When: Adding item to cart
        mockMvc.perform(post("/api/cart/items")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Item is added successfully
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].code", is("P100")))
                .andExpect(jsonPath("$.items[0].name", is("Test Product")))
                .andExpect(jsonPath("$.items[0].price", is(29.99)))
                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.items[0].subtotal", is(59.98)))
                .andExpect(jsonPath("$.totalAmount", is(59.98)))
                .andExpect(jsonPath("$.itemCount", is(2))); // itemCount is total quantity, not distinct items
    }

    @Test
    void shouldReturnNotFoundWhenAddingNonExistentProduct() throws Exception {
        // Given: Product does not exist
        when(productApi.getByCode(anyString())).thenReturn(Optional.empty());

        AddToCartRequest request = new AddToCartRequest("INVALID", 1);
        MockHttpSession session = new MockHttpSession();

        // When: Adding non-existent product
        mockMvc.perform(post("/api/cart/items")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Returns 404 Not Found
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestForInvalidAddToCartRequest() throws Exception {
        // Given: Invalid request (quantity < 1)
        AddToCartRequest request = new AddToCartRequest("P100", 0);
        MockHttpSession session = new MockHttpSession();

        // When: Sending invalid request
        mockMvc.perform(post("/api/cart/items")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Returns 400 Bad Request
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateItemQuantity() throws Exception {
        // Given: Cart has an item
        ProductDto product = new ProductDto(
                "P100", "Test Product", "A test product description", "test.jpg", new BigDecimal("29.99"));
        when(productApi.getByCode("P100")).thenReturn(Optional.of(product));

        MockHttpSession session = new MockHttpSession();

        // Add item first
        AddToCartRequest addRequest = new AddToCartRequest("P100", 2);
        mockMvc.perform(post("/api/cart/items")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)));

        // When: Updating quantity
        UpdateQuantityRequest updateRequest = new UpdateQuantityRequest(5);
        mockMvc.perform(put("/api/cart/items/{code}", "P100")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                // Then: Quantity is updated
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity", is(5)))
                .andExpect(jsonPath("$.items[0].subtotal", is(149.95)))
                .andExpect(jsonPath("$.totalAmount", is(149.95)));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentItem() throws Exception {
        // Given: Empty cart
        MockHttpSession session = new MockHttpSession();

        // When: Updating non-existent item
        UpdateQuantityRequest request = new UpdateQuantityRequest(3);
        mockMvc.perform(put("/api/cart/items/{code}", "P100")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: Returns 404 Not Found
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestForInvalidQuantityUpdate() throws Exception {
        // Given: Cart has an item
        ProductDto product = new ProductDto(
                "P100", "Test Product", "A test product description", "test.jpg", new BigDecimal("29.99"));
        when(productApi.getByCode("P100")).thenReturn(Optional.of(product));

        MockHttpSession session = new MockHttpSession();

        // Add item first
        AddToCartRequest addRequest = new AddToCartRequest("P100", 2);
        mockMvc.perform(post("/api/cart/items")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)));

        // When: Updating with invalid quantity (< 1)
        UpdateQuantityRequest updateRequest = new UpdateQuantityRequest(0);
        mockMvc.perform(put("/api/cart/items/{code}", "P100")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                // Then: Returns 400 Bad Request
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetCart() throws Exception {
        // Given: Cart has items
        ProductDto product = new ProductDto(
                "P100", "Test Product", "A test product description", "test.jpg", new BigDecimal("29.99"));
        when(productApi.getByCode("P100")).thenReturn(Optional.of(product));

        MockHttpSession session = new MockHttpSession();

        // Add item first
        AddToCartRequest addRequest = new AddToCartRequest("P100", 2);
        mockMvc.perform(post("/api/cart/items")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)));

        // When: Getting cart
        mockMvc.perform(get("/api/cart").session(session))
                // Then: Returns cart contents
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].code", is("P100")))
                .andExpect(jsonPath("$.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.totalAmount", is(59.98)))
                .andExpect(jsonPath("$.itemCount", is(2))); // itemCount is total quantity
    }

    @Test
    void shouldGetEmptyCart() throws Exception {
        // Given: Empty cart
        MockHttpSession session = new MockHttpSession();

        // When: Getting cart
        mockMvc.perform(get("/api/cart").session(session))
                // Then: Returns empty cart
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalAmount", is(0)))
                .andExpect(jsonPath("$.itemCount", is(0)));
    }

    @Test
    void shouldClearCart() throws Exception {
        // Given: Cart has items
        ProductDto product = new ProductDto(
                "P100", "Test Product", "A test product description", "test.jpg", new BigDecimal("29.99"));
        when(productApi.getByCode("P100")).thenReturn(Optional.of(product));

        MockHttpSession session = new MockHttpSession();

        // Add item first
        AddToCartRequest addRequest = new AddToCartRequest("P100", 2);
        mockMvc.perform(post("/api/cart/items")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)));

        // When: Clearing cart
        mockMvc.perform(delete("/api/cart").session(session))
                // Then: Cart is cleared
                .andExpect(status().isNoContent());

        // Verify cart is empty
        mockMvc.perform(get("/api/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalAmount", is(0)))
                .andExpect(jsonPath("$.itemCount", is(0)));
    }

    @Test
    void shouldHandleMultipleSessionsIndependently() throws Exception {
        // Given: Two different sessions
        ProductDto product = new ProductDto(
                "P100", "Test Product", "A test product description", "test.jpg", new BigDecimal("29.99"));
        when(productApi.getByCode("P100")).thenReturn(Optional.of(product));

        MockHttpSession session1 = new MockHttpSession();
        MockHttpSession session2 = new MockHttpSession();

        AddToCartRequest request = new AddToCartRequest("P100", 2);

        // When: Adding items to different sessions
        mockMvc.perform(post("/api/cart/items")
                .session(session1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Then: Each session has independent cart
        mockMvc.perform(get("/api/cart").session(session1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount", is(2))); // itemCount is total quantity, not distinct items

        mockMvc.perform(get("/api/cart").session(session2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount", is(0)));
    }
}
