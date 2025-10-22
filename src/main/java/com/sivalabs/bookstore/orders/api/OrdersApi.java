package com.sivalabs.bookstore.orders.api;

import com.sivalabs.bookstore.common.models.PagedResult;
import java.util.Optional;

/**
 * Public facade exposing order operations to other modules.
 */
public interface OrdersApi {

    CreateOrderResponse createOrder(CreateOrderRequest request);

    Optional<OrderDto> findOrder(String orderNumber);

    PagedResult<OrderView> findOrders(int page, int size);
}
