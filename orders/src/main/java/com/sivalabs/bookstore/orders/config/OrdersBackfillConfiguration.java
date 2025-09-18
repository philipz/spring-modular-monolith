package com.sivalabs.bookstore.orders.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(BackfillProperties.class)
public class OrdersBackfillConfiguration {

    @Bean(name = "ordersBackfillDataSource", destroyMethod = "close")
    @ConditionalOnProperty(prefix = "orders.backfill.source", name = "url")
    public DataSource ordersBackfillDataSource(BackfillProperties properties) {
        BackfillProperties.Source source = properties.getSource();
        if (!StringUtils.hasText(source.getUrl())) {
            throw new IllegalStateException("orders.backfill.source.url must not be blank");
        }
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(source.getUrl())
                .username(source.getUsername())
                .password(source.getPassword())
                .driverClassName(source.getDriverClassName())
                .build();
    }

    @Bean(name = "legacyOrdersJdbcTemplate")
    @ConditionalOnProperty(prefix = "orders.backfill", name = "enabled", havingValue = "true")
    public JdbcTemplate legacyOrdersJdbcTemplate(
            @Qualifier("ordersBackfillDataSource") ObjectProvider<DataSource> sourceDataSourceProvider,
            DataSource dataSource) {
        DataSource source = sourceDataSourceProvider.getIfAvailable(() -> dataSource);
        return new JdbcTemplate(source);
    }
}
