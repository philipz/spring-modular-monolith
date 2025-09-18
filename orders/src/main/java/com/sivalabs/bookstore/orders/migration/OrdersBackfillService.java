package com.sivalabs.bookstore.orders.migration;

import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.api.model.OrderItem;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import com.sivalabs.bookstore.orders.domain.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "orders.backfill", name = "enabled", havingValue = "true")
public class OrdersBackfillService {

    private final LegacyOrderReader legacyOrderReader;
    private final OrderRepository orderRepository;

    @Transactional
    public int runBackfill(BackfillRequest request) {
        List<LegacyOrderRecord> records = legacyOrderReader.fetchOrders(request);
        if (records.isEmpty()) {
            log.info("No legacy orders found for backfill");
            return 0;
        }

        Set<String> existingOrderNumbers =
                orderRepository
                        .findByOrderNumberIn(records.stream()
                                .map(LegacyOrderRecord::orderNumber)
                                .toList())
                        .stream()
                        .map(OrderEntity::getOrderNumber)
                        .collect(Collectors.toSet());

        int processed = 0;
        for (LegacyOrderRecord record : records) {
            if (existingOrderNumbers.contains(record.orderNumber())) {
                continue;
            }
            OrderEntity entity = mapToEntity(record);
            orderRepository.save(entity);
            processed++;
        }

        log.info("Orders backfill processed {} legacy orders", processed);
        return processed;
    }

    private OrderEntity mapToEntity(LegacyOrderRecord record) {
        OrderStatus status = resolveStatus(record.status());
        OrderEntity entity = OrderEntity.builder()
                .orderNumber(record.orderNumber())
                .customer(new Customer(record.customerName(), record.customerEmail(), record.customerPhone()))
                .deliveryAddress(record.deliveryAddress())
                .orderItem(new OrderItem(
                        record.productCode(), record.productName(), record.productPrice(), record.quantity()))
                .status(status)
                .build();

        LocalDateTime createdAt = record.createdAt() != null ? record.createdAt() : LocalDateTime.now();
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(record.updatedAt() != null ? record.updatedAt() : createdAt);
        return entity;
    }

    private OrderStatus resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return OrderStatus.NEW;
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Unexpected order status '{}' found during backfill. Defaulting to NEW.", status);
            return OrderStatus.NEW;
        }
    }
}
