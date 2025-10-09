package com.sivalabs.bookstore.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import liquibase.integration.spring.SpringLiquibase;

/**
 * Liquibase configuration for database migration management.
 *
 * This configuration creates a SpringLiquibase instance and configures
 * the database migration with appropriate settings for the bookstore application.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LiquibaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(LiquibaseConfig.class);

    @Value("${spring.liquibase.change-log:classpath:db/db.changelog-master.xml}")
    private String changeLog;

    @Value("${spring.liquibase.contexts:}")
    private String contexts;

    @Value("${spring.liquibase.default-schema:}")
    private String defaultSchema;

    @Value("${spring.liquibase.drop-first:false}")
    private boolean dropFirst;

    /**
     * Creates and configures the SpringLiquibase bean for database migrations.
     *
     * @param dataSource the DataSource to use for migrations
     * @return configured SpringLiquibase instance
     */
    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        logger.info("Initializing Liquibase configuration");

        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(changeLog);

        if (!contexts.isEmpty()) {
            liquibase.setContexts(contexts);
            logger.info("Liquibase contexts configured: {}", contexts);
        }

        if (!defaultSchema.isEmpty()) {
            liquibase.setDefaultSchema(defaultSchema);
            logger.info("Liquibase default schema configured: {}", defaultSchema);
        }

        liquibase.setDropFirst(dropFirst);
        if (dropFirst) {
            logger.warn("Liquibase drop-first is enabled - database will be dropped and recreated!");
        }

        // Enable validation and rollback support
        liquibase.setShouldRun(true);

        logger.info(
                "Liquibase configuration completed - Change log: {}, Drop first: {}, Default schema: {}",
                changeLog,
                dropFirst,
                defaultSchema.isEmpty() ? "not set" : defaultSchema);

        return liquibase;
    }
}
