package com.sivalabs.bookstore.orders.web.cart;

import com.sivalabs.bookstore.catalog.api.ProductApi;
import com.sivalabs.bookstore.catalog.api.ProductDto;
import com.sivalabs.bookstore.orders.web.cart.dto.AddToCartRequest;
import com.sivalabs.bookstore.orders.web.cart.dto.CartDto;
import com.sivalabs.bookstore.orders.web.cart.dto.UpdateQuantityRequest;
import com.sivalabs.bookstore.orders.web.cart.mapper.CartMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping cart management API")
public class CartRestController {

    private final ProductApi productApi;

    public CartRestController(ProductApi productApi) {
        this.productApi = productApi;
    }

    @PostMapping("/items")
    @Operation(
            summary = "Add item to cart",
            description = "Adds a product to the shopping cart or updates quantity if already exists")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Item added to cart successfully",
                content = @Content(schema = @Schema(implementation = CartDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<CartDto> addItem(@Valid @RequestBody AddToCartRequest request, HttpSession session) {
        Cart cart = CartUtil.getCart(session);

        ProductDto product = productApi
                .getByCode(request.code())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product with code '" + request.code() + "' not found"));

        Cart.LineItem lineItem = new Cart.LineItem(product.code(), product.name(), product.price(), request.quantity());

        cart.setItem(lineItem);
        CartUtil.setCart(session, cart);

        CartDto cartDto = CartMapper.toDto(cart);
        return ResponseEntity.status(HttpStatus.CREATED).body(cartDto);
    }

    @PutMapping("/items/{code}")
    @Operation(summary = "Update item quantity", description = "Updates the quantity of an item in the cart")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Item quantity updated successfully",
                content = @Content(schema = @Schema(implementation = CartDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid quantity"),
        @ApiResponse(responseCode = "404", description = "Item not found in cart")
    })
    public ResponseEntity<CartDto> updateItemQuantity(
            @PathVariable String code, @Valid @RequestBody UpdateQuantityRequest request, HttpSession session) {
        Cart cart = CartUtil.getCart(session);

        if (cart.getItem() == null || !cart.getItem().getCode().equals(code)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item with code '" + code + "' not found in cart");
        }

        cart.updateItemQuantity(request.quantity());
        CartUtil.setCart(session, cart);

        CartDto cartDto = CartMapper.toDto(cart);
        return ResponseEntity.ok(cartDto);
    }

    @GetMapping
    @Operation(summary = "Get cart", description = "Retrieves the current shopping cart contents")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Cart retrieved successfully",
                content = @Content(schema = @Schema(implementation = CartDto.class)))
    })
    public ResponseEntity<CartDto> getCart(HttpSession session) {
        Cart cart = CartUtil.getCart(session);
        CartDto cartDto = CartMapper.toDto(cart);
        return ResponseEntity.ok(cartDto);
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Removes all items from the shopping cart")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Cart cleared successfully")})
    public ResponseEntity<Void> clearCart(HttpSession session) {
        Cart cart = CartUtil.getCart(session);
        cart.removeItem();
        CartUtil.setCart(session, cart);
        return ResponseEntity.noContent().build();
    }
}
