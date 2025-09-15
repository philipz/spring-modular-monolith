package com.sivalabs.bookstore.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("CacheProperties Configuration Binding Tests")
class CachePropertiesTests {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(CacheProperties.class)
    static class TestConfig {}

    @Test
    @DisplayName("Binds defaults including inventory TTL when not overridden")
    void shouldBindDefaultInventoryTtl() {
        contextRunner.run(ctx -> {
            CacheProperties props = ctx.getBean(CacheProperties.class);
            assertThat(props).isNotNull();
            assertThat(props.getTimeToLiveSeconds()).isEqualTo(3600);
            assertThat(props.getInventoryTimeToLiveSeconds()).isEqualTo(1800);
        });
    }

    @Test
    @DisplayName("Binds overridden inventory TTL from properties")
    void shouldBindOverriddenInventoryTtl() {
        contextRunner
                .withPropertyValues("bookstore.cache.inventory-time-to-live-seconds=1234")
                .run(ctx -> {
                    CacheProperties props = ctx.getBean(CacheProperties.class);
                    assertThat(props.getInventoryTimeToLiveSeconds()).isEqualTo(1234);
                });
    }
}
