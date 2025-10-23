package com.sivalabs.bookstore.orders.api;

import com.sivalabs.bookstore.common.models.PagedResult;

/**
 * Remote client abstraction for interacting with the Orders service via non-local transports (e.g. gRPC).
 */
public interface OrdersRemoteClient {

    CreateOrderResponse createOrder(CreateOrderRequest request);

    OrderDto getOrder(String orderNumber);

    PagedResult<OrderView> listOrders(int page, int size);
}
