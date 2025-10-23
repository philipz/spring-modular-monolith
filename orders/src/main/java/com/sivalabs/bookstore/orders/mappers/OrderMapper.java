package com.sivalabs.bookstore.orders.mappers;

import com.sivalabs.bookstore.common.models.PagedResult;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import com.sivalabs.bookstore.orders.domain.OrderEntity;
import java.util.UUID;

public final class OrderMapper {

    private OrderMapper() {}

    public static OrderEntity convertToEntity(CreateOrderRequest request) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderNumber(UUID.randomUUID().toString());
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

    public static OrderView convertToOrderView(OrderEntity order) {
        return new OrderView(order.getOrderNumber(), order.getStatus(), order.getCustomer());
    }

    public static PagedResult<OrderView> convertToOrderViewPage(PagedResult<OrderEntity> orders) {
        return PagedResult.of(orders, OrderMapper::convertToOrderView);
    }
}
