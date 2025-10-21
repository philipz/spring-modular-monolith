package com.sivalabs.bookstore.orders.web.cart.mapper;

import com.sivalabs.bookstore.orders.web.cart.Cart;
import com.sivalabs.bookstore.orders.web.cart.dto.CartDto;
import com.sivalabs.bookstore.orders.web.cart.dto.CartItemDto;
import java.math.BigDecimal;
import java.util.List;

public final class CartMapper {

    private CartMapper() {}

    public static CartDto toDto(Cart cart) {
        if (cart == null || cart.getItem() == null) {
            return new CartDto(List.of(), BigDecimal.ZERO, 0);
        }

        CartItemDto itemDto = toItemDto(cart.getItem());
        return new CartDto(List.of(itemDto), itemDto.subtotal(), itemDto.quantity());
    }

    public static CartItemDto toItemDto(Cart.LineItem lineItem) {
        if (lineItem == null) {
            return null;
        }

        BigDecimal subtotal = lineItem.getPrice().multiply(BigDecimal.valueOf(lineItem.getQuantity()));

        return new CartItemDto(
                lineItem.getCode(), lineItem.getName(), lineItem.getPrice(), lineItem.getQuantity(), subtotal);
    }

    public static Cart.LineItem toLineItem(CartItemDto itemDto) {
        if (itemDto == null) {
            return null;
        }

        return new Cart.LineItem(itemDto.code(), itemDto.name(), itemDto.price(), itemDto.quantity());
    }
}
