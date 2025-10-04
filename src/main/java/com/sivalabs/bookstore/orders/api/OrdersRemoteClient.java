package com.sivalabs.bookstore.orders.api;

import java.util.List;

/**
 * Remote client abstraction for interacting with the Orders service via non-local transports (e.g. gRPC).
 */
public interface OrdersRemoteClient {

    CreateOrderResponse createOrder(CreateOrderRequest request);

    OrderDto getOrder(String orderNumber);

    List<OrderView> listOrders();
}
