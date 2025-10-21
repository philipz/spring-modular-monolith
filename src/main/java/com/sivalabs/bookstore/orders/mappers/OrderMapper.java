package com.sivalabs.bookstore.orders.mappers;

import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class OrderMapper {
    private OrderMapper() {}

    public static OrderEntity convertToEntity(CreateOrderRequest request) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderNumber("ORD-" + UUID.randomUUID().toString());
        entity.setStatus(OrderStatus.NEW);
        entity.setCustomer(request.customer());
        entity.setDeliveryAddress(request.deliveryAddress());
        entity.setOrderItem(request.item());
        return entity;
    }

    public static OrderDto convertToDto(OrderEntity order) {
        return new OrderDto(
                order.getOrderNumber(),
                order.getOrderItem(),
                order.getCustomer(),
                order.getDeliveryAddress(),
                order.getStatus(),
                order.getCreatedAt());
    }

    public static List<OrderView> convertToOrderViews(List<OrderEntity> orders) {
        List<OrderView> orderViews = new ArrayList<>();
        for (OrderEntity order : orders) {
            var orderView = new OrderView(order.getOrderNumber(), order.getStatus(), order.getCustomer());
            orderViews.add(orderView);
        }
        return orderViews;
    }
}
