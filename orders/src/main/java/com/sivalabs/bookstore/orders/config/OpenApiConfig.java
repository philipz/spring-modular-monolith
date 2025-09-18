package com.sivalabs.bookstore.orders.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI ordersOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Orders Service API")
                        .description("Orders microservice extracted from the bookstore modular monolith")
                        .version("1.0.0")
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
                .addServersItem(new Server().url("http://localhost:8091").description("Development server"));
    }
}
