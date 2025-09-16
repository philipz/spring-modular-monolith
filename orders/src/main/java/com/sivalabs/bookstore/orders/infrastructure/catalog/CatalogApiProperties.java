package com.sivalabs.bookstore.orders.infrastructure.catalog;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "catalog.api")
public record CatalogApiProperties(
        URI baseUrl, @DefaultValue("PT1S") Duration connectTimeout, @DefaultValue("PT2S") Duration readTimeout) {

    public CatalogApiProperties {
        if (baseUrl == null) {
            baseUrl = URI.create("http://localhost:8080");
        }
    }
}
