package com.sivalabs.bookstore.orders.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

@DisplayName("CartUtil JSON Serialization Tests")
class CartUtilTests {

    @Test
    @DisplayName("Should serialize and deserialize cart with JSON successfully")
    void shouldSerializeAndDeserializeCartWithJson() {
        // Given
        HttpSession session = new MockHttpSession();
        Cart originalCart = new Cart();
        Cart.LineItem item = new Cart.LineItem("P001", "Test Product", new BigDecimal("19.99"), 2);
        originalCart.setItem(item);

        // When
        CartUtil.setCart(session, originalCart);
        Cart retrievedCart = CartUtil.getCart(session);

        // Then
        assertThat(retrievedCart).isNotNull();
        assertThat(retrievedCart.getItem()).isNotNull();
        assertThat(retrievedCart.getItem().getCode()).isEqualTo("P001");
        assertThat(retrievedCart.getItem().getName()).isEqualTo("Test Product");
        assertThat(retrievedCart.getItem().getPrice()).isEqualTo(new BigDecimal("19.99"));
        assertThat(retrievedCart.getItem().getQuantity()).isEqualTo(2);
        assertThat(retrievedCart.getTotalAmount()).isEqualTo(new BigDecimal("39.98"));
    }

    @Test
    @DisplayName("Should handle empty cart JSON serialization")
    void shouldHandleEmptyCartJsonSerialization() {
        // Given
        HttpSession session = new MockHttpSession();
        Cart emptyCart = new Cart();

        // When
        CartUtil.setCart(session, emptyCart);
        Cart retrievedCart = CartUtil.getCart(session);

        // Then
        assertThat(retrievedCart).isNotNull();
        assertThat(retrievedCart.getItem()).isNull();
        assertThat(retrievedCart.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should create new cart when no cart exists in session")
    void shouldCreateNewCartWhenNoCartExistsInSession() {
        // Given
        HttpSession session = new MockHttpSession();

        // When
        Cart cart = CartUtil.getCart(session);

        // Then
        assertThat(cart).isNotNull();
        assertThat(cart.getItem()).isNull();

        // Verify cart is stored as JSON
        String cartJson = (String) session.getAttribute("cart_json");
        assertThat(cartJson).isNotNull().contains("\"item\":null");
    }

    @Test
    @DisplayName("Should migrate direct Cart instance to JSON format (Phase 2)")
    void shouldMigrateDirectCartInstanceToJsonFormat() {
        // Given
        HttpSession session = new MockHttpSession();
        Cart legacyCart = new Cart();
        Cart.LineItem item = new Cart.LineItem("P002", "Legacy Product", new BigDecimal("29.99"), 1);
        legacyCart.setItem(item);

        // Simulate legacy cart storage (direct Cart object)
        session.setAttribute("cart", legacyCart);

        // When
        Cart retrievedCart = CartUtil.getCart(session);

        // Then
        assertThat(retrievedCart).isNotNull();
        assertThat(retrievedCart.getItem()).isNotNull();
        assertThat(retrievedCart.getItem().getCode()).isEqualTo("P002");
        assertThat(retrievedCart.getItem().getName()).isEqualTo("Legacy Product");

        // Verify migration to JSON format
        String cartJson = (String) session.getAttribute("cart_json");
        assertThat(cartJson).isNotNull().contains("P002");

        // Verify legacy cart object is removed
        Object legacyCartObj = session.getAttribute("cart");
        assertThat(legacyCartObj).isNull();
    }

    @Test
    @DisplayName("Should discard incompatible legacy cart and create new one (Phase 2)")
    void shouldDiscardIncompatibleLegacyCartAndCreateNew() {
        // Given
        HttpSession session = new MockHttpSession();

        // Simulate an incompatible object in cart attribute (e.g., from different ClassLoader)
        // Using String as an example of incompatible object
        session.setAttribute("cart", "incompatible_cart_object");

        // When
        Cart retrievedCart = CartUtil.getCart(session);

        // Then
        assertThat(retrievedCart).isNotNull();
        assertThat(retrievedCart.getItem()).isNull(); // Should be new empty cart

        // Verify new cart is stored as JSON
        String cartJson = (String) session.getAttribute("cart_json");
        assertThat(cartJson).isNotNull().contains("\"item\":null");

        // Verify incompatible legacy object is removed
        Object legacyCartObj = session.getAttribute("cart");
        assertThat(legacyCartObj).isNull();
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void shouldHandleMalformedJsonGracefully() {
        // Given
        HttpSession session = new MockHttpSession();
        session.setAttribute("cart_json", "{invalid_json}");

        // When
        Cart cart = CartUtil.getCart(session);

        // Then
        assertThat(cart).isNotNull();
        assertThat(cart.getItem()).isNull(); // Should create new empty cart
    }

    @Test
    @DisplayName("Should preserve cart state through multiple operations")
    void shouldPreserveCartStateThroughMultipleOperations() {
        // Given
        HttpSession session = new MockHttpSession();
        Cart cart = new Cart();

        // When - multiple cart operations
        cart.setItem(new Cart.LineItem("P001", "Product 1", new BigDecimal("10.00"), 1));
        CartUtil.setCart(session, cart);

        Cart retrieved1 = CartUtil.getCart(session);
        retrieved1.updateItemQuantity(3);
        CartUtil.setCart(session, retrieved1);

        Cart retrieved2 = CartUtil.getCart(session);

        // Then
        assertThat(retrieved2.getItem().getQuantity()).isEqualTo(3);
        assertThat(retrieved2.getTotalAmount()).isEqualTo(new BigDecimal("30.00"));
    }
}
