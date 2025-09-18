package com.sivalabs.bookstore.orders.migration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LegacyOrderRecord(
        String orderNumber,
        String customerName,
        String customerEmail,
        String customerPhone,
        String deliveryAddress,
        String productCode,
        String productName,
        BigDecimal productPrice,
        int quantity,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
