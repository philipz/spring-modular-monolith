package com.sivalabs.bookstore.orders.web;

import com.sivalabs.bookstore.orders.web.dto.AddToCartRequest;
import com.sivalabs.bookstore.orders.web.dto.ApiResponse;
import com.sivalabs.bookstore.orders.web.dto.CartDto;
import com.sivalabs.bookstore.orders.web.dto.CartItemDto;
import com.sivalabs.bookstore.orders.web.dto.CartMapper;
import com.sivalabs.bookstore.orders.web.dto.UpdateQuantityRequest;
import com.sivalabs.bookstore.orders.web.exception.CartNotFoundException;
import com.sivalabs.bookstore.orders.web.exception.InvalidCartOperationException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
class CartRestController {

    private static final Logger log = LoggerFactory.getLogger(CartRestController.class);

    private final ProductApiAdapter productApiAdapter;

    CartRestController(ProductApiAdapter productApiAdapter) {
        this.productApiAdapter = productApiAdapter;
    }

    @GetMapping
    ResponseEntity<ApiResponse<CartDto>> getCart(HttpSession session) {
        Cart cart = CartUtil.getCart(session);
        log.debug("Fetching cart for sessionId={}, hasItem={} ", session.getId(), cart.getItem() != null);
        CartDto cartDto = CartMapper.toDto(cart);
        return ResponseEntity.ok(ApiResponse.of(cartDto, "Cart retrieved"));
    }

    @PostMapping("/items")
    ResponseEntity<ApiResponse<CartDto>> addItem(@Valid @RequestBody AddToCartRequest request, HttpSession session) {
        Cart cart = CartUtil.getCart(session);
        log.info("Adding product {} to cart, sessionId={}", request.productCode(), session.getId());

        var product = productApiAdapter
                .getByCode(request.productCode())
                .orElseThrow(() -> InvalidCartOperationException.productUnavailable(request.productCode()));

        CartItemDto item = new CartItemDto(product.code(), product.name(), product.price(), 1);
        CartMapper.replaceItem(cart, item);
        CartUtil.setCart(session, cart);
        CartDto cartDto = CartMapper.toDto(cart);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(cartDto, "Item added to cart"));
    }

    @PutMapping("/items/{productCode}")
    ResponseEntity<ApiResponse<CartDto>> updateItemQuantity(
            @PathVariable String productCode, @Valid @RequestBody UpdateQuantityRequest request, HttpSession session) {
        Cart cart = CartUtil.getCart(session);
        if (cart.getItem() == null) {
            throw CartNotFoundException.forSession(session.getId());
        }
        if (!cart.getItem().getCode().equals(productCode)) {
            throw InvalidCartOperationException.productMismatch(
                    productCode, cart.getItem().getCode());
        }

        log.info(
                "Updating quantity for product {}, requestedQuantity={}, sessionId={}",
                productCode,
                request.quantity(),
                session.getId());

        if (request.quantity() == null) {
            throw InvalidCartOperationException.quantityNotProvided();
        }

        if (request.quantity() <= 0) {
            cart.removeItem();
        } else {
            CartMapper.applyQuantity(cart, request.quantity());
        }

        CartUtil.setCart(session, cart);
        CartDto cartDto = CartMapper.toDto(cart);
        return ResponseEntity.ok(ApiResponse.of(cartDto, "Cart updated"));
    }
}
