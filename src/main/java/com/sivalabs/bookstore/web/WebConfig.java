package com.sivalabs.bookstore.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for web-related beans.
 */
@Configuration
public class WebConfig {

    /**
     * Provides a RestTemplate bean for making HTTP requests to external services.
     *
     * @return RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
