package com.sivalabs.bookstore.orders.grpc;

import com.sivalabs.bookstore.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        properties = {
            "bookstore.cache.enabled=false",
            "bookstore.session.hazelcast.enabled=false",
            "app.amqp.new-orders.bind=false",
            "spring.rabbitmq.listener.direct.auto-startup=false",
            "spring.rabbitmq.listener.simple.auto-startup=false",
            "grpc.server.port=-1"
        },
        classes = {
            com.sivalabs.bookstore.BookStoreApplication.class,
            com.sivalabs.bookstore.testsupport.session.TestSessionConfiguration.class
        })
@Import(TestcontainersConfiguration.class)
@DisplayName("Orders gRPC Service Tests")
class OrdersGrpcServiceTests {

    @Test
    void contextLoads() {
        // Placeholder test to bootstrap Spring context; real tests will be added later.
    }
}
