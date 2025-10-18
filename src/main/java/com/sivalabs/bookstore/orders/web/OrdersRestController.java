package com.sivalabs.bookstore.orders.web;

import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.OrdersRemoteClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management API")
public class OrdersRestController {
    private final OrdersRemoteClient ordersRemoteClient;

    public OrdersRestController(OrdersRemoteClient ordersRemoteClient) {
        this.ordersRemoteClient = ordersRemoteClient;
    }

    @PostMapping
    @Operation(summary = "Create order", description = "Creates a new order from the provided order details")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Order created successfully",
                content = @Content(schema = @Schema(implementation = CreateOrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid order data"),
        @ApiResponse(responseCode = "503", description = "Orders service unavailable")
    })
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        try {
            CreateOrderResponse response = ordersRemoteClient.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException(ex);
        }
    }

    @GetMapping
    @Operation(summary = "List orders", description = "Retrieves a list of all orders")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Orders retrieved successfully",
                content = @Content(schema = @Schema(implementation = OrderView.class))),
        @ApiResponse(responseCode = "503", description = "Orders service unavailable")
    })
    public ResponseEntity<List<OrderView>> listOrders() {
        try {
            List<OrderView> orders = ordersRemoteClient.listOrders();
            return ResponseEntity.ok(orders);
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException(ex);
        }
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get order details", description = "Retrieves detailed information about a specific order")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Order retrieved successfully",
                content = @Content(schema = @Schema(implementation = OrderDto.class))),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "503", description = "Orders service unavailable")
    })
    public ResponseEntity<OrderDto> getOrder(@PathVariable String orderNumber) {
        try {
            OrderDto order = ordersRemoteClient.getOrder(orderNumber);
            return ResponseEntity.ok(order);
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException(ex);
        }
    }

    /**
     * Maps gRPC StatusRuntimeException to appropriate HTTP ResponseStatusException
     *
     * @param exception the gRPC exception
     * @return ResponseStatusException with appropriate HTTP status and message
     */
    private ResponseStatusException mapGrpcException(StatusRuntimeException exception) {
        Status status = exception.getStatus();
        String description = status.getDescription() != null
                ? status.getDescription()
                : status.getCode().name();

        return switch (status.getCode()) {
            case NOT_FOUND -> new ResponseStatusException(HttpStatus.NOT_FOUND, description);
            case INVALID_ARGUMENT -> new ResponseStatusException(HttpStatus.BAD_REQUEST, description);
            case UNAVAILABLE ->
                new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "Orders service unavailable. Please try again later.");
            case DEADLINE_EXCEEDED ->
                new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Request timeout. Please try again later.");
            default ->
                new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Unable to process request at the moment. Please try again later.");
        };
    }
}
