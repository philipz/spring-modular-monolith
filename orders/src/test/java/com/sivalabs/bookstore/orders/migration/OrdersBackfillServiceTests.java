package com.sivalabs.bookstore.orders.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrdersBackfillServiceTests {

    @Mock
    private LegacyOrderReader legacyOrderReader;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrdersBackfillService service;

    @Test
    @DisplayName("Should skip processing when no legacy orders are returned")
    void shouldSkipWhenNoLegacyOrders() {
        BackfillRequest request = new BackfillRequest(LocalDateTime.now().minusDays(7), 50);
        given(legacyOrderReader.fetchOrders(request)).willReturn(List.of());

        int processed = service.runBackfill(request);

        assertThat(processed).isZero();
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Should persist new orders and ignore existing ones")
    void shouldPersistNewOrders() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(3);
        LegacyOrderRecord newOrder = new LegacyOrderRecord(
                "ORD-NEW",
                "Alice",
                "alice@example.com",
                "+11111111",
                "123 Street",
                "P100",
                "Sample Book",
                new BigDecimal("19.99"),
                2,
                "NEW",
                createdAt,
                createdAt.plusHours(2));

        LegacyOrderRecord existingOrder = new LegacyOrderRecord(
                "ORD-EXIST",
                "Bob",
                "bob@example.com",
                "+22222222",
                "456 Avenue",
                "P200",
                "Existing Book",
                new BigDecimal("29.99"),
                1,
                "SHIPPED",
                createdAt,
                createdAt.plusHours(1));

        BackfillRequest request = new BackfillRequest(null, 10);

        given(legacyOrderReader.fetchOrders(request)).willReturn(List.of(newOrder, existingOrder));
        given(orderRepository.findByOrderNumberIn(List.of("ORD-NEW", "ORD-EXIST")))
                .willReturn(List.of(OrderEntity.builder()
                        .orderNumber("ORD-EXIST")
                        .status(OrderStatus.NEW)
                        .build()));

        int processed = service.runBackfill(request);

        assertThat(processed).isEqualTo(1);

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        OrderEntity saved = captor.getValue();
        assertThat(saved.getOrderNumber()).isEqualTo("ORD-NEW");
        assertThat(saved.getCustomer().email()).isEqualTo("alice@example.com");
        assertThat(saved.getOrderItem().code()).isEqualTo("P100");
        assertThat(saved.getOrderItem().price()).isEqualTo(new BigDecimal("19.99"));
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        assertThat(saved.getUpdatedAt()).isEqualTo(createdAt.plusHours(2));
    }
}
