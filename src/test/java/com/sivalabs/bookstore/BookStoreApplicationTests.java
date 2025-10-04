package com.sivalabs.bookstore;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {"bookstore.grpc.server.port=0"})
@Import(TestcontainersConfiguration.class)
class BookStoreApplicationTests {

    @Test
    void contextLoads() {}
}
