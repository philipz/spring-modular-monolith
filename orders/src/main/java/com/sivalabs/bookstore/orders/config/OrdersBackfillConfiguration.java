package com.sivalabs.bookstore.orders.config;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(BackfillProperties.class)
public class OrdersBackfillConfiguration {

    @Bean(name = "legacyOrdersJdbcTemplate")
    @ConditionalOnProperty(prefix = "orders.backfill", name = "enabled", havingValue = "true")
    public JdbcTemplate legacyOrdersJdbcTemplate(BackfillProperties properties, DataSource dataSource) {
        BackfillProperties.Source source = properties.getSource();
        if (!StringUtils.hasText(source.getUrl())) {
            return new JdbcTemplate(dataSource);
        }

        DriverManagerDataSource legacyDataSource = new DriverManagerDataSource();
        legacyDataSource.setUrl(source.getUrl());
        legacyDataSource.setUsername(source.getUsername());
        legacyDataSource.setPassword(source.getPassword());
        legacyDataSource.setDriverClassName(source.getDriverClassName());
        return new JdbcTemplate(legacyDataSource);
    }
}
