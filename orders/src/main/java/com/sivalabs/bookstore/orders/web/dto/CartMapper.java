package com.sivalabs.bookstore.orders.web.dto;

import com.sivalabs.bookstore.orders.web.Cart;
import java.math.BigDecimal;

/**
 * Helper methods for converting between Cart session model and REST DTOs.
 */
public final class CartMapper {

    private CartMapper() {}

    public static CartDto toDto(Cart cart) {
        if (cart == null || cart.getItem() == null) {
            return new CartDto(null, BigDecimal.ZERO);
        }
        return new CartDto(toDto(cart.getItem()), cart.getTotalAmount());
    }

    public static CartItemDto toDto(Cart.LineItem lineItem) {
        if (lineItem == null) {
            return null;
        }
        return new CartItemDto(lineItem.getCode(), lineItem.getName(), lineItem.getPrice(), lineItem.getQuantity());
    }

    public static void applyQuantity(Cart cart, int quantity) {
        if (cart == null || cart.getItem() == null) {
            return;
        }
        cart.updateItemQuantity(quantity);
    }

    public static void replaceItem(Cart cart, CartItemDto itemDto) {
        if (cart == null) {
            return;
        }
        if (itemDto == null) {
            cart.removeItem();
            return;
        }
        cart.setItem(new Cart.LineItem(itemDto.code(), itemDto.name(), itemDto.price(), itemDto.quantity()));
    }
}
