package com.sivalabs.bookstore.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sivalabs.bookstore.orders.api.events.OrderCreatedEvent;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceUnitTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private OrderCachePort orderCachePort;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, eventPublisher, orderCachePort);
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully with valid entity")
        void shouldCreateOrderSuccessfullyWithValidEntity() {
            // Given
            OrderEntity orderEntity = createValidOrderEntity();
            OrderEntity savedOrder = createSavedOrderEntity();

            given(orderRepository.save(any(OrderEntity.class))).willReturn(savedOrder);

            // When
            OrderEntity result = orderService.createOrder(orderEntity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrderNumber()).isEqualTo(savedOrder.getOrderNumber());

            ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
            verify(orderRepository).save(orderCaptor.capture());

            OrderEntity capturedOrder = orderCaptor.getValue();
            assertThat(capturedOrder.getCustomer().email())
                    .isEqualTo(orderEntity.getCustomer().email());
            assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.NEW);
            assertThat(capturedOrder.getDeliveryAddress()).isEqualTo(orderEntity.getDeliveryAddress());
        }

        @Test
        @DisplayName("Should publish event when order created")
        void shouldPublishEventWhenOrderCreated() {
            // Given
            OrderEntity orderEntity = createValidOrderEntity();
            OrderEntity savedOrder = createSavedOrderEntity();

            given(orderRepository.save(any(OrderEntity.class))).willReturn(savedOrder);

            // When
            orderService.createOrder(orderEntity);

            // Then
            verify(eventPublisher).publishEvent(any(com.sivalabs.bookstore.orders.api.events.OrderCreatedEvent.class));
        }

        @Test
        @DisplayName("Should publish OrderCreatedEvent with expected payload")
        void shouldPublishOrderCreatedEventWithExpectedPayload() {
            // Given
            OrderEntity orderEntity = createValidOrderEntity();
            OrderEntity savedOrder = createSavedOrderEntity("ORD-123456", "PROD-001", 3);

            given(orderRepository.save(any(OrderEntity.class))).willReturn(savedOrder);

            // When
            orderService.createOrder(orderEntity);

            // Then
            ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            OrderCreatedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.orderNumber()).isEqualTo(savedOrder.getOrderNumber());
            assertThat(publishedEvent.productCode())
                    .isEqualTo(savedOrder.getOrderItem().code());
            assertThat(publishedEvent.quantity())
                    .isEqualTo(savedOrder.getOrderItem().quantity());
            assertThat(publishedEvent.customer()).isEqualTo(savedOrder.getCustomer());
        }

        @Test
        @DisplayName("Should generate unique order number")
        void shouldGenerateUniqueOrderNumber() {
            // Given
            OrderEntity orderEntity = createValidOrderEntity();
            OrderEntity savedOrder = createSavedOrderEntity();

            given(orderRepository.save(any(OrderEntity.class))).willReturn(savedOrder);

            // When
            OrderEntity result = orderService.createOrder(orderEntity);

            // Then
            assertThat(result.getOrderNumber()).isNotBlank();
            assertThat(result.getOrderNumber()).hasSize(36); // UUID length
        }
    }

    @Nested
    @DisplayName("Find Order Tests")
    class FindOrderTests {

        @Test
        @DisplayName("Should find order by order number")
        void shouldFindOrderByOrderNumber() {
            // Given
            String orderNumber = "test-order-123";
            OrderEntity existingOrder = createOrderEntityWithOrderNumber(orderNumber);

            given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.of(existingOrder));

            // When
            Optional<OrderEntity> result = orderService.findOrder(orderNumber);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(existingOrder);
            verify(orderRepository).findByOrderNumber(orderNumber);
        }

        @Test
        @DisplayName("Should return empty when order not found")
        void shouldReturnEmptyWhenOrderNotFound() {
            // Given
            String orderNumber = "non-existent-order";

            given(orderRepository.findByOrderNumber(orderNumber)).willReturn(Optional.empty());

            // When
            Optional<OrderEntity> result = orderService.findOrder(orderNumber);

            // Then
            assertThat(result).isEmpty();
            verify(orderRepository).findByOrderNumber(orderNumber);
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should throw exception for null entity")
        void shouldThrowExceptionForNullEntity() {
            // When & Then
            assertThatThrownBy(() -> orderService.createOrder(null)).isInstanceOf(NullPointerException.class);

            verify(orderRepository, never()).save(any(OrderEntity.class));
        }
    }

    // Helper methods
    private OrderEntity createValidOrderEntity() {
        Customer customer = new Customer("John Doe", "john@example.com", "+1234567890");
        OrderItem orderItem = new OrderItem("PROD-001", "Test Product", BigDecimal.valueOf(99.99), 1);
        return new OrderEntity(
                null,
                null, // Let the service generate the order number
                customer,
                "123 Test Street",
                orderItem,
                OrderStatus.NEW,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private OrderEntity createSavedOrderEntity() {
        return createSavedOrderEntity(java.util.UUID.randomUUID().toString(), "PROD-001", 1);
    }

    private OrderEntity createSavedOrderEntity(String orderNumber, String productCode, int quantity) {
        Customer customer = new Customer("John Doe", "john@example.com", "+1234567890");
        OrderItem orderItem = new OrderItem(productCode, "Test Product", BigDecimal.valueOf(99.99), quantity);
        return new OrderEntity(
                1L,
                orderNumber,
                customer,
                "123 Test Street",
                orderItem,
                OrderStatus.NEW,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private OrderEntity createOrderEntityWithOrderNumber(String orderNumber) {
        Customer customer = new Customer("John Doe", "john@example.com", "+1234567890");
        OrderItem orderItem = new OrderItem("PROD-001", "Test Product", BigDecimal.valueOf(99.99), 1);

        return new OrderEntity(
                1L,
                orderNumber,
                customer,
                "123 Test Street",
                orderItem,
                OrderStatus.NEW,
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
