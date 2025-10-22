package com.sivalabs.bookstore.orders;

import com.sivalabs.bookstore.common.models.PagedResult;
import com.sivalabs.bookstore.orders.api.CreateOrderRequest;
import com.sivalabs.bookstore.orders.api.CreateOrderResponse;
import com.sivalabs.bookstore.orders.api.OrderDto;
import com.sivalabs.bookstore.orders.api.OrderView;
import com.sivalabs.bookstore.orders.api.OrdersApi;
import com.sivalabs.bookstore.orders.domain.OrderService;
import com.sivalabs.bookstore.orders.domain.ProductCatalogPort;
import com.sivalabs.bookstore.orders.mappers.OrderMapper;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class OrdersApiService implements OrdersApi {

    private final OrderService orderService;
    private final ProductCatalogPort productCatalogPort;

    OrdersApiService(OrderService orderService, ProductCatalogPort productCatalogPort) {
        this.orderService = orderService;
        this.productCatalogPort = productCatalogPort;
    }

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        // Validate business rules beyond basic field validation
        validateOrderItem(request.item());

        productCatalogPort.validate(request.item().code(), request.item().price());
        var savedOrder = orderService.createOrder(OrderMapper.convertToEntity(request));
        return new CreateOrderResponse(savedOrder.getOrderNumber());
    }

    private void validateOrderItem(com.sivalabs.bookstore.orders.api.model.OrderItem item) {
        if (item.quantity() <= 0) {
            throw new InvalidOrderException("Quantity must be greater than 0");
        }
        if (item.price().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Price must be greater than 0");
        }
        if (item.code() == null || item.code().trim().isEmpty()) {
            throw new InvalidOrderException("Product code is required");
        }
        if (item.name() == null || item.name().trim().isEmpty()) {
            throw new InvalidOrderException("Product name is required");
        }
    }

    @Override
    public Optional<OrderDto> findOrder(String orderNumber) {
        return orderService.findOrder(orderNumber).map(OrderMapper::convertToDto);
    }

    @Override
    public PagedResult<OrderView> findOrders(int page, int size) {
        int pageNumber = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        PageRequest pageRequest =
                PageRequest.of(pageNumber - 1, pageSize, Sort.by("id").descending());
        var pageResult = orderService.findOrders(pageRequest);
        var viewPage = pageResult.map(OrderMapper::convertToOrderView);
        return new PagedResult<>(
                viewPage.getContent(),
                pageResult.getTotalElements(),
                viewPage.getNumber() + 1,
                viewPage.getTotalPages(),
                viewPage.isFirst(),
                viewPage.isLast(),
                viewPage.hasNext(),
                viewPage.hasPrevious());
    }
}
