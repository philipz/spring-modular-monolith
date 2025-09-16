package com.sivalabs.bookstore.orders.web;

import com.sivalabs.bookstore.orders.OrderNotFoundException;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.OrdersApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders", description = "Order management operations")
@RestController
@RequestMapping("/api/orders")
class OrderRestController {

    private static final Logger log = LoggerFactory.getLogger(OrderRestController.class);

    private final OrdersApi ordersApi;

    OrderRestController(OrdersApi ordersApi) {
        this.ordersApi = ordersApi;
    }

    @Operation(
            summary = "Create a new order",
            description = "Creates a new order with customer details and order items")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Order created successfully",
                        content = @Content(schema = @Schema(implementation = CreateOrderResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid order request", content = @Content)
            })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ordersApi.createOrder(request);
    }

    @Operation(summary = "Get order by order number", description = "Retrieves an order by its unique order number")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Order found",
                        content = @Content(schema = @Schema(implementation = OrderDto.class))),
                @ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
            })
    @GetMapping("/{orderNumber}")
    OrderDto getOrder(
            @Parameter(description = "Unique order number", required = true) @PathVariable String orderNumber) {
        log.info("Fetching order by orderNumber: {}", orderNumber);
        return ordersApi.findOrder(orderNumber).orElseThrow(() -> OrderNotFoundException.forOrderNumber(orderNumber));
    }

    @Operation(summary = "Get all orders", description = "Retrieves a list of all orders")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Orders retrieved successfully",
                        content = @Content(schema = @Schema(implementation = OrderView.class)))
            })
    @GetMapping
    List<OrderView> getOrders() {
        return ordersApi.findOrders();
    }
}
