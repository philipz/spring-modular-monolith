package com.sivalabs.bookstore.orders.web.cart;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CartUtil {
    private static final Logger log = LoggerFactory.getLogger(CartUtil.class);
    private static final String CART_JSON_KEY = "cart_json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private CartUtil() {}

    public static Cart getCart(HttpSession session) {
        String cartJson = (String) session.getAttribute(CART_JSON_KEY);
        if (cartJson != null) {
            try {
                Cart cart = objectMapper.readValue(cartJson, Cart.class);
                log.debug(
                        "Successfully deserialized cart from JSON - sessionId: {}, item: {}",
                        session.getId(),
                        cart.getItem() != null ? cart.getItem().getCode() : "null");
                return cart;
            } catch (JsonProcessingException e) {
                log.warn(
                        "Failed to deserialize cart from JSON - sessionId: {}, error: {}",
                        session.getId(),
                        e.getMessage());
            }
        }

        Object legacyCart = session.getAttribute("cart");
        if (legacyCart instanceof Cart cart) {
            log.info("Migrating legacy Cart instance to JSON - sessionId: {}", session.getId());
            setCart(session, cart);
            session.removeAttribute("cart");
            return cart;
        }

        if (legacyCart != null) {
            log.warn(
                    "Found incompatible legacy cart object, discarding - sessionId: {}, type: {}",
                    session.getId(),
                    legacyCart.getClass().getName());
            session.removeAttribute("cart");
        }

        log.info("Creating new cart for session: {}", session.getId());
        Cart cart = new Cart();
        setCart(session, cart);
        return cart;
    }

    public static void setCart(HttpSession session, Cart cart) {
        try {
            String cartJson = objectMapper.writeValueAsString(cart);
            session.setAttribute(CART_JSON_KEY, cartJson);
            log.debug("Successfully serialized cart to JSON - sessionId: {}", session.getId());
        } catch (JsonProcessingException e) {
            log.error("Critical error: Failed to serialize cart to JSON - sessionId: {}", session.getId(), e);
            Cart emptyCart = new Cart();
            try {
                String emptyCartJson = objectMapper.writeValueAsString(emptyCart);
                session.setAttribute(CART_JSON_KEY, emptyCartJson);
                log.warn("Created empty cart due to serialization failure - sessionId: {}", session.getId());
            } catch (JsonProcessingException ex) {
                log.error("Fatal error: Cannot serialize even empty cart - sessionId: {}", session.getId(), ex);
                throw new RuntimeException("Cart serialization system failure", ex);
            }
        }
    }
}
