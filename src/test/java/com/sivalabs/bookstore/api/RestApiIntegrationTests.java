package com.sivalabs.bookstore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.sivalabs.bookstore.TestcontainersConfiguration;
import com.sivalabs.bookstore.catalog.api.ProductDto;
import com.sivalabs.bookstore.common.models.ErrorResponse;
import com.sivalabs.bookstore.common.models.PagedResult;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.web.cart.dto.AddToCartRequest;
import com.sivalabs.bookstore.orders.web.cart.dto.CartDto;
import com.sivalabs.bookstore.orders.web.cart.dto.UpdateQuantityRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration tests for REST API endpoints.
 * Tests the complete request-response cycle with real HTTP requests.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Sql("/test-products-data.sql")
class RestApiIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterEach
    void cleanupCart() {
        // Clear cart after each test to ensure test isolation
        try {
            restTemplate.delete("/api/cart");
        } catch (Exception e) {
            // Ignore errors if cart is already empty
        }
    }

    // ========== Products API Tests ==========

    @Test
    void shouldGetAllProducts() {
        ResponseEntity<PagedResult<ProductDto>> response = restTemplate.exchange(
                "/api/products", HttpMethod.GET, null, new ParameterizedTypeReference<PagedResult<ProductDto>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PagedResult<ProductDto> result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(10);
        assertThat(result.totalElements()).isEqualTo(15);
        assertThat(result.pageNumber()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isFalse();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void shouldGetProductsPaginated() {
        ResponseEntity<PagedResult<ProductDto>> response = restTemplate.exchange(
                "/api/products?page=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<PagedResult<ProductDto>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PagedResult<ProductDto> result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(5);
        assertThat(result.pageNumber()).isEqualTo(2);
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void shouldGetProductByCode() {
        ResponseEntity<ProductDto> response = restTemplate.getForEntity("/api/products/P100", ProductDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ProductDto product = response.getBody();
        assertThat(product).isNotNull();
        assertThat(product.code()).isEqualTo("P100");
        assertThat(product.name()).isEqualTo("The Hunger Games");
        assertThat(product.price()).isEqualByComparingTo(new BigDecimal("34.0"));
    }

    @Test
    void shouldReturn404WhenProductNotFound() {
        ResponseEntity<ErrorResponse> response =
                restTemplate.getForEntity("/api/products/INVALID_CODE", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(404);
        assertThat(error.message()).contains("INVALID_CODE");
    }

    // ========== Cart API Tests ==========
    // NOTE: Cart tests are disabled because TestRestTemplate doesn't maintain HTTP sessions.
    // These tests should be rewritten using MockMvc or @WebMvcTest for proper session support.

    @Test
    @Disabled("TestRestTemplate doesn't maintain HTTP sessions - use MockMvc instead")
    void shouldAddItemToCart() {
        AddToCartRequest request = new AddToCartRequest("P100", 2);

        ResponseEntity<CartDto> response = restTemplate.postForEntity("/api/cart/items", request, CartDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CartDto cart = response.getBody();
        assertThat(cart).isNotNull();
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).code()).isEqualTo("P100");
        assertThat(cart.items().get(0).quantity()).isEqualTo(2);
        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.totalAmount()).isEqualByComparingTo(new BigDecimal("68.0"));
    }

    @Test
    @Disabled("TestRestTemplate doesn't maintain HTTP sessions - use MockMvc instead")
    void shouldReturn404WhenAddingInvalidProductToCart() {
        AddToCartRequest request = new AddToCartRequest("INVALID_CODE", 1);

        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/api/cart/items", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(404);
        assertThat(error.message()).contains("INVALID_CODE");
    }

    @Test
    @Disabled("TestRestTemplate doesn't maintain HTTP sessions - use MockMvc instead")
    void shouldUpdateCartItemQuantity() {
        // First add item to cart
        AddToCartRequest addRequest = new AddToCartRequest("P100", 1);
        restTemplate.postForEntity("/api/cart/items", addRequest, CartDto.class);

        // Update quantity
        UpdateQuantityRequest updateRequest = new UpdateQuantityRequest(5);
        HttpEntity<UpdateQuantityRequest> requestEntity = new HttpEntity<>(updateRequest);

        ResponseEntity<CartDto> response =
                restTemplate.exchange("/api/cart/items/P100", HttpMethod.PUT, requestEntity, CartDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CartDto cart = response.getBody();
        assertThat(cart).isNotNull();
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).quantity()).isEqualTo(5);
        assertThat(cart.totalAmount()).isEqualByComparingTo(new BigDecimal("170.0"));
    }

    @Test
    @Disabled("TestRestTemplate doesn't maintain HTTP sessions - use MockMvc instead")
    void shouldGetCart() {
        // Add items to cart
        restTemplate.postForEntity("/api/cart/items", new AddToCartRequest("P100", 2), CartDto.class);

        // Get cart
        ResponseEntity<CartDto> response = restTemplate.getForEntity("/api/cart", CartDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CartDto cart = response.getBody();
        assertThat(cart).isNotNull();
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.itemCount()).isEqualTo(1);
    }

    @Test
    @Disabled("TestRestTemplate doesn't maintain HTTP sessions - use MockMvc instead")
    void shouldClearCart() {
        // Add items to cart
        restTemplate.postForEntity("/api/cart/items", new AddToCartRequest("P100", 2), CartDto.class);

        // Clear cart
        restTemplate.delete("/api/cart");

        // Verify cart is empty
        ResponseEntity<CartDto> response = restTemplate.getForEntity("/api/cart", CartDto.class);
        CartDto cart = response.getBody();
        assertThat(cart).isNotNull();
        assertThat(cart.items()).isEmpty();
        assertThat(cart.itemCount()).isZero();
        assertThat(cart.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @Disabled("TestRestTemplate doesn't maintain HTTP sessions - use MockMvc instead")
    void shouldMaintainCartAcrossRequests() {
        // Add first item
        restTemplate.postForEntity("/api/cart/items", new AddToCartRequest("P100", 1), CartDto.class);

        // Get cart and verify
        ResponseEntity<CartDto> response1 = restTemplate.getForEntity("/api/cart", CartDto.class);
        assertThat(response1.getBody().items()).hasSize(1);

        // Clear and verify
        restTemplate.delete("/api/cart");
        ResponseEntity<CartDto> response2 = restTemplate.getForEntity("/api/cart", CartDto.class);
        assertThat(response2.getBody().items()).isEmpty();
    }

    // ========== Orders API Tests ==========

    @Test
    void shouldCreateOrder() {
        Customer customer = new Customer("John Doe", "john.doe@example.com", "+1-555-123-4567");
        OrderItem item = new OrderItem("P100", "The Hunger Games", new BigDecimal("34.0"), 2);
        CreateOrderRequest request = new CreateOrderRequest(customer, "742 Evergreen Terrace, Springfield", item);

        ResponseEntity<CreateOrderResponse> response =
                restTemplate.postForEntity("/api/orders", request, CreateOrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CreateOrderResponse orderResponse = response.getBody();
        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.orderNumber()).isNotEmpty();
        assertThat(orderResponse.orderNumber()).startsWith("ORD-");
    }

    @Test
    void shouldReturn400ForInvalidOrderData() {
        // Invalid email
        Customer customer = new Customer("John Doe", "invalid-email", "+1-555-123-4567");
        OrderItem item = new OrderItem("P100", "The Hunger Games", new BigDecimal("34.0"), 2);
        CreateOrderRequest request = new CreateOrderRequest(customer, "742 Evergreen Terrace", item);

        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/api/orders", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(400);
        assertThat(error.message()).contains("Validation failed");
    }

    @Test
    void shouldListOrders() {
        // Create an order first
        Customer customer = new Customer("John Doe", "john.doe@example.com", "+1-555-123-4567");
        OrderItem item = new OrderItem("P100", "The Hunger Games", new BigDecimal("34.0"), 1);
        CreateOrderRequest createRequest = new CreateOrderRequest(customer, "742 Evergreen Terrace", item);
        restTemplate.postForEntity("/api/orders", createRequest, CreateOrderResponse.class);

        // List orders
        ResponseEntity<OrderView[]> response = restTemplate.getForEntity("/api/orders", OrderView[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OrderView[] orders = response.getBody();
        assertThat(orders).isNotNull();
        assertThat(orders).hasSizeGreaterThanOrEqualTo(1);
        assertThat(orders[0].orderNumber()).isNotEmpty();
        assertThat(orders[0].customer()).isNotNull();
    }

    @Test
    void shouldGetOrderByOrderNumber() {
        // Create an order first
        Customer customer = new Customer("Jane Smith", "jane.smith@example.com", "+1-555-987-6543");
        OrderItem item = new OrderItem("P101", "To Kill a Mockingbird", new BigDecimal("45.40"), 1);
        CreateOrderRequest createRequest = new CreateOrderRequest(customer, "123 Main Street", item);
        ResponseEntity<CreateOrderResponse> createResponse =
                restTemplate.postForEntity("/api/orders", createRequest, CreateOrderResponse.class);

        String orderNumber = createResponse.getBody().orderNumber();

        // Get order by order number
        ResponseEntity<OrderDto> response = restTemplate.getForEntity("/api/orders/" + orderNumber, OrderDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OrderDto order = response.getBody();
        assertThat(order).isNotNull();
        assertThat(order.orderNumber()).isEqualTo(orderNumber);
        assertThat(order.customer().name()).isEqualTo("Jane Smith");
        assertThat(order.item().code()).isEqualTo("P101");
        assertThat(order.deliveryAddress()).isEqualTo("123 Main Street");
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("45.40"));
    }

    @Test
    void shouldReturn404WhenOrderNotFound() {
        ResponseEntity<ErrorResponse> response =
                restTemplate.getForEntity("/api/orders/INVALID-ORDER-NUMBER", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(404);
    }

    // ========== End-to-End Shopping Flow Test ==========

    @Test
    @Disabled("TestRestTemplate doesn't maintain HTTP sessions - use MockMvc instead")
    void shouldCompleteFullShoppingFlow() {
        // 1. Browse products
        ResponseEntity<PagedResult<ProductDto>> productsResponse = restTemplate.exchange(
                "/api/products", HttpMethod.GET, null, new ParameterizedTypeReference<PagedResult<ProductDto>>() {});
        assertThat(productsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. Add items to cart
        restTemplate.postForEntity("/api/cart/items", new AddToCartRequest("P100", 2), CartDto.class);
        ResponseEntity<CartDto> cartResponse = restTemplate.getForEntity("/api/cart", CartDto.class);
        assertThat(cartResponse.getBody().items()).hasSize(1);

        // 3. Create order from cart
        CartDto cart = cartResponse.getBody();
        Customer customer = new Customer("Test User", "test.user@example.com", "+1-555-000-0000");
        OrderItem orderItem = new OrderItem(
                cart.items().get(0).code(),
                cart.items().get(0).name(),
                cart.items().get(0).price(),
                cart.items().get(0).quantity());
        CreateOrderRequest orderRequest = new CreateOrderRequest(customer, "Test Address", orderItem);

        ResponseEntity<CreateOrderResponse> orderResponse =
                restTemplate.postForEntity("/api/orders", orderRequest, CreateOrderResponse.class);
        assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 4. Verify order was created
        String orderNumber = orderResponse.getBody().orderNumber();
        ResponseEntity<OrderDto> orderDetailsResponse =
                restTemplate.getForEntity("/api/orders/" + orderNumber, OrderDto.class);
        assertThat(orderDetailsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderDetailsResponse.getBody().orderNumber()).isEqualTo(orderNumber);

        // 5. Clear cart after order
        restTemplate.delete("/api/cart");
        ResponseEntity<CartDto> emptyCartResponse = restTemplate.getForEntity("/api/cart", CartDto.class);
        assertThat(emptyCartResponse.getBody().items()).isEmpty();
    }
}
