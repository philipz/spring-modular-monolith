package com.sivalabs.bookstore.orders;

import com.sivalabs.bookstore.common.models.PagedResult;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.OrdersApi;
import com.sivalabs.bookstore.orders.domain.OrderService;
import com.sivalabs.bookstore.orders.domain.ProductServiceClient;
import com.sivalabs.bookstore.orders.mappers.OrderMapper;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OrdersApiService implements OrdersApi {

    private final OrderService orderService;
    private final ProductServiceClient productServiceClient;

    OrdersApiService(OrderService orderService, ProductServiceClient productServiceClient) {
        this.orderService = orderService;
        this.productServiceClient = productServiceClient;
    }

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        productServiceClient.validate(request.item().code(), request.item().price());
        var savedOrder = orderService.createOrder(OrderMapper.convertToEntity(request));
        return new CreateOrderResponse(savedOrder.getOrderNumber());
    }

    @Override
    public Optional<OrderDto> findOrder(String orderNumber) {
        return orderService.findOrder(orderNumber).map(OrderMapper::convertToDto);
    }

    @Override
    public PagedResult<OrderView> findOrders(int page, int size) {
        return OrderMapper.convertToOrderViewPage(orderService.findOrders(page, size));
    }
}
