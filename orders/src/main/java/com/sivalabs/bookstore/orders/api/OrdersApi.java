package com.sivalabs.bookstore.orders.api;

import java.util.List;
import java.util.Optional;

public interface OrdersApi {

    CreateOrderResponse createOrder(CreateOrderRequest request);

    Optional<OrderDto> findOrder(String orderNumber);

    List<OrderView> findOrders();
}
