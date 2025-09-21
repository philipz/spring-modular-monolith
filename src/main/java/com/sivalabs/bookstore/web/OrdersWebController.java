package com.sivalabs.bookstore.web;

import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;

/**
 * Controller to handle /orders path by displaying orders using Thymeleaf template.
 * This fetches data from the external Orders Service API and renders using local templates.
 */
@Controller
public class OrdersWebController {

    private static final Logger log = LoggerFactory.getLogger(OrdersWebController.class);

    @Value("${orders.service.api-url}")
    private String ordersServiceApiUrl;

    private final RestTemplate restTemplate;

    public OrdersWebController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/orders")
    public String createOrder(
            @ModelAttribute @Valid OrderForm orderForm, BindingResult bindingResult, Model model, HttpSession session) {
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
            log.info("Creating order via external service: {}", ordersServiceApiUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<CreateOrderResponse> response =
                    restTemplate.exchange(ordersServiceApiUrl, HttpMethod.POST, entity, CreateOrderResponse.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                CreateOrderResponse savedOrder = response.getBody();
                session.removeAttribute("cart");
                CartUtil.setCart(session, new Cart());
                String orderNumber = savedOrder.orderNumber();
                log.info("Successfully created order: {}", orderNumber);
                return "redirect:/orders/" + orderNumber;
            } else {
                log.error("Failed to create order, unexpected response status: {}", response.getStatusCode());
                model.addAttribute("cart", cart);
                model.addAttribute("error", "Failed to create order. Please try again.");
                return "cart";
            }

        } catch (Exception e) {
            log.error("Failed to create order via external service: {}", e.getMessage());
            model.addAttribute("cart", cart);
            model.addAttribute("error", "Unable to create order. Please try again later.");
            return "cart";
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
    public String showOrders(Model model, HtmxRequest hxRequest) {
        log.info("Fetching orders from external service: {}", ordersServiceApiUrl);

        try {
            ResponseEntity<OrderView[]> response =
                    restTemplate.exchange(ordersServiceApiUrl, HttpMethod.GET, null, OrderView[].class);

            List<OrderView> orders =
                    response.getBody() != null ? Arrays.asList(response.getBody()) : Collections.emptyList();

            model.addAttribute("orders", orders);
            log.info("Successfully fetched {} orders from external service", orders.size());

        } catch (Exception e) {
            log.error("Failed to fetch orders from external service: {}", e.getMessage());
            model.addAttribute("orders", Collections.emptyList());
            model.addAttribute("error", "Unable to load orders. Please try again later.");
        }

        if (hxRequest.isHtmxRequest()) {
            return "partials/orders";
        }
        return "orders";
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
    public String showOrderDetails(@PathVariable String orderNumber, Model model) {
        log.info("Fetching order details for orderNumber: {} from external service", orderNumber);

        try {
            String orderDetailsUrl = ordersServiceApiUrl + "/" + orderNumber;
            ResponseEntity<OrderDto> response =
                    restTemplate.exchange(orderDetailsUrl, HttpMethod.GET, null, OrderDto.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                model.addAttribute("order", response.getBody());
                log.info("Successfully fetched order details for orderNumber: {}", orderNumber);
            } else {
                log.warn("Order not found for orderNumber: {}", orderNumber);
                model.addAttribute("order", null);
            }

        } catch (Exception e) {
            log.error("Failed to fetch order details for orderNumber: {} - {}", orderNumber, e.getMessage());
            model.addAttribute("order", null);
            model.addAttribute("error", "Unable to load order details. Please try again later.");
        }

        return "order_details";
    }
}
