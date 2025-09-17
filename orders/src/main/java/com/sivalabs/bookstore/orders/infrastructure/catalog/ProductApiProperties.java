package com.sivalabs.bookstore.orders.infrastructure.catalog;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "product.api")
public record ProductApiProperties(
        URI baseUrl, @DefaultValue("PT1S") Duration connectTimeout, @DefaultValue("PT2S") Duration readTimeout) {

    public ProductApiProperties {
        if (baseUrl == null) {
            baseUrl = URI.create("http://localhost:8080");
        }
    }
}
