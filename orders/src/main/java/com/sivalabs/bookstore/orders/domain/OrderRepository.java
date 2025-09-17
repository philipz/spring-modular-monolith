package com.sivalabs.bookstore.orders.domain;

import com.sivalabs.bookstore.orders.api.model.OrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {
    @Query("""
        select distinct o
        from OrderEntity o left join fetch o.orderItem
        """)
    List<OrderEntity> findAllBy(Sort sort);

    @Query(
            value = """
        select distinct o
        from OrderEntity o left join fetch o.orderItem
        """,
            countQuery = "select count(o) from OrderEntity o")
    Page<OrderEntity> findAllWithItems(Pageable pageable);

    @Query(
            """
        select distinct o
        from OrderEntity o left join fetch o.orderItem
        where o.orderNumber = :orderNumber
        """)
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    List<OrderEntity> findByOrderNumberIn(Collection<String> orderNumbers);

    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);

    @Query("""
        select o from OrderEntity o
        where o.customer.email = :email
        """)
    Page<OrderEntity> findByCustomerEmail(String email, Pageable pageable);
}
