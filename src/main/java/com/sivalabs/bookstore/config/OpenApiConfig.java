package com.sivalabs.bookstore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation configuration for the BookStore REST API.
 *
 * <p>This configuration provides programmatic customization of the OpenAPI specification, including:
 * - API metadata (title, version, description)
 * - Server information
 * - License details
 *
 * <p>The OpenAPI specification is available at /api-docs and the Swagger UI at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI bookstoreOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BookStore REST API")
                        .description("REST API for BookStore modular monolith application. "
                                + "Provides endpoints for managing products, shopping cart, and orders. "
                                + "Built with Spring Boot and Spring Modulith.")
                        .version("1.0.0")
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addServersItem(
                        new Server().url("http://localhost:" + serverPort).description("Local development server"));
    }
}
