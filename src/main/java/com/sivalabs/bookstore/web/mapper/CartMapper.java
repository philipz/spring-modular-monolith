package com.sivalabs.bookstore.web.mapper;

import com.sivalabs.bookstore.web.Cart;
import com.sivalabs.bookstore.web.dto.CartDto;
import com.sivalabs.bookstore.web.dto.CartItemDto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class CartMapper {

    private CartMapper() {
        // Utility class, prevent instantiation
    }

    /**
     * Convert Cart domain model to CartDto
     *
     * @param cart the cart domain model
     * @return CartDto with mapped data
     */
    public static CartDto toDto(Cart cart) {
        if (cart == null) {
            return new CartDto(List.of(), BigDecimal.ZERO, 0);
        }

        List<CartItemDto> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int itemCount = 0;

        if (cart.getItem() != null) {
            CartItemDto itemDto = toItemDto(cart.getItem());
            items.add(itemDto);
            totalAmount = itemDto.subtotal();
            itemCount = itemDto.quantity();
        }

        return new CartDto(items, totalAmount, itemCount);
    }

    /**
     * Convert Cart.LineItem to CartItemDto
     *
     * @param lineItem the cart line item
     * @return CartItemDto with calculated subtotal
     */
    public static CartItemDto toItemDto(Cart.LineItem lineItem) {
        if (lineItem == null) {
            return null;
        }

        BigDecimal subtotal = lineItem.getPrice().multiply(BigDecimal.valueOf(lineItem.getQuantity()));

        return new CartItemDto(
                lineItem.getCode(), lineItem.getName(), lineItem.getPrice(), lineItem.getQuantity(), subtotal);
    }

    /**
     * Convert CartItemDto back to Cart.LineItem
     *
     * @param itemDto the cart item DTO
     * @return Cart.LineItem domain object
     */
    public static Cart.LineItem toLineItem(CartItemDto itemDto) {
        if (itemDto == null) {
            return null;
        }

        return new Cart.LineItem(itemDto.code(), itemDto.name(), itemDto.price(), itemDto.quantity());
    }
}
