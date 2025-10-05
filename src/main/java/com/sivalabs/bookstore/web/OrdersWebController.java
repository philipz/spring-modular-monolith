package com.sivalabs.bookstore.web;

import com.sivalabs.bookstore.orders.InvalidOrderException;
import com.sivalabs.bookstore.orders.OrderNotFoundException;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.OrdersRemoteClient;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller to handle /orders path using the Orders gRPC client while rendering
 * responses with Thymeleaf templates.
 */
@Controller
public class OrdersWebController {

    private static final Logger log = LoggerFactory.getLogger(OrdersWebController.class);

    private final OrdersRemoteClient ordersClient;

    public OrdersWebController(OrdersRemoteClient ordersClient) {
        this.ordersClient = ordersClient;
    }

    @PostMapping("/orders")
    public String createOrder(
            @ModelAttribute @Valid OrderForm orderForm,
            BindingResult bindingResult,
            Model model,
            HttpSession session,
            HttpServletResponse response) {
        Cart cart = CartUtil.getCart(session);
        if (bindingResult.hasErrors()) {
            model.addAttribute("cart", cart);
            return "cart";
        }

        Cart.LineItem lineItem = cart.getItem();
        OrderItem orderItem =
                new OrderItem(lineItem.getCode(), lineItem.getName(), lineItem.getPrice(), lineItem.getQuantity());
        CreateOrderRequest request =
                new CreateOrderRequest(orderForm.customer(), orderForm.deliveryAddress(), orderItem);

        try {
            log.info("Creating order via gRPC client");
            CreateOrderResponse savedOrder = ordersClient.createOrder(request);
            session.removeAttribute("cart");
            CartUtil.setCart(session, new Cart());
            String orderNumber = savedOrder.orderNumber();
            log.info("Successfully created order: {}", orderNumber);
            return "redirect:/orders/" + orderNumber;
        } catch (InvalidOrderException ex) {
            log.warn("Invalid order request: {}", ex.getMessage());
            return renderError(HttpStatus.BAD_REQUEST, ex.getMessage(), model, response);
        } catch (OrderNotFoundException ex) {
            log.warn("Referenced order not found during creation: {}", ex.getMessage());
            return renderError(HttpStatus.NOT_FOUND, ex.getMessage(), model, response);
        } catch (StatusRuntimeException ex) {
            log.error("gRPC error while creating order", ex);
            return handleGrpcStatusException(ex, model, response);
        } catch (Exception ex) {
            log.error("Unexpected error while creating order", ex);
            return renderError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to create order. Please try again later.",
                    model,
                    response);
        }
    }

    /**
     * Handles GET requests to /orders by displaying the orders page.
     * Fetches orders from the external Orders Service API and renders them using Thymeleaf.
     *
     * @param model the model to populate with orders data
     * @param hxRequest HTMX request information for partial rendering
     * @return the view name to render
     */
    @GetMapping("/orders")
    public String showOrders(Model model, HtmxRequest hxRequest, HttpServletResponse response) {
        log.info("Fetching orders via gRPC client");

        try {
            List<OrderView> orders = ordersClient.listOrders();
            model.addAttribute("orders", orders);
            log.info("Successfully fetched {} orders", orders.size());

            if (hxRequest.isHtmxRequest()) {
                return "partials/orders";
            }
            return "orders";
        } catch (InvalidOrderException ex) {
            log.warn("Invalid request while listing orders: {}", ex.getMessage());
            return renderError(HttpStatus.BAD_REQUEST, ex.getMessage(), model, response);
        } catch (OrderNotFoundException ex) {
            log.warn("Orders not found: {}", ex.getMessage());
            return renderError(HttpStatus.NOT_FOUND, ex.getMessage(), model, response);
        } catch (StatusRuntimeException ex) {
            log.error("gRPC error while fetching orders", ex);
            return handleGrpcStatusException(ex, model, response);
        } catch (Exception ex) {
            log.error("Unexpected error while fetching orders", ex);
            return renderError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load orders. Please try again later.",
                    model,
                    response);
        }
    }

    /**
     * Handles GET requests to /orders/{orderNumber} by displaying the order details page.
     * Fetches order details from the external Orders Service API and renders them using Thymeleaf.
     *
     * @param orderNumber the order number to fetch
     * @param model the model to populate with order data
     * @return the view name to render
     */
    @GetMapping("/orders/{orderNumber}")
    public String showOrderDetails(@PathVariable String orderNumber, Model model, HttpServletResponse response) {
        log.info("Fetching order details for orderNumber: {} via gRPC client", orderNumber);

        try {
            OrderDto order = ordersClient.getOrder(orderNumber);
            model.addAttribute("order", order);
            return "order_details";
        } catch (OrderNotFoundException ex) {
            log.warn("Order not found: {}", orderNumber);
            return renderError(HttpStatus.NOT_FOUND, ex.getMessage(), model, response);
        } catch (InvalidOrderException ex) {
            log.warn("Invalid request while fetching order details: {}", ex.getMessage());
            return renderError(HttpStatus.BAD_REQUEST, ex.getMessage(), model, response);
        } catch (StatusRuntimeException ex) {
            log.error("gRPC error while fetching order details", ex);
            return handleGrpcStatusException(ex, model, response);
        } catch (Exception ex) {
            log.error("Unexpected error while fetching order details", ex);
            return renderError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load order details. Please try again later.",
                    model,
                    response);
        }
    }

    private String handleGrpcStatusException(
            StatusRuntimeException exception, Model model, HttpServletResponse response) {
        Status status = exception.getStatus();
        String description = status.getDescription() != null
                ? status.getDescription()
                : status.getCode().name();
        return switch (status.getCode()) {
            case NOT_FOUND -> renderError(HttpStatus.NOT_FOUND, description, model, response);
            case INVALID_ARGUMENT -> renderError(HttpStatus.BAD_REQUEST, description, model, response);
            default ->
                renderError(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Unable to process request at the moment. Please try again later.",
                        model,
                        response);
        };
    }

    private String renderError(HttpStatus status, String message, Model model, HttpServletResponse response) {
        response.setStatus(status.value());
        model.addAttribute("message", message);
        return switch (status) {
            case BAD_REQUEST -> "error/400";
            case NOT_FOUND -> "error/404";
            default -> "error/500";
        };
    }
}
