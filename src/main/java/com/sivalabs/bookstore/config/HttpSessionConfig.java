package com.sivalabs.bookstore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.hazelcast.config.annotation.web.http.EnableHazelcastHttpSession;

/**
 * Configuration for enabling Hazelcast-based HTTP session management.
 *
 * This configuration enables distributed session storage using the existing
 * Hazelcast cluster, allowing session data to be shared across multiple
 * application instances in a horizontally scaled deployment.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "bookstore.session.hazelcast",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableHazelcastHttpSession(
        maxInactiveIntervalInSeconds = 1800 // 30 minutes session timeout
        )
public class HttpSessionConfig {

    private static final Logger logger = LoggerFactory.getLogger(HttpSessionConfig.class);

    public HttpSessionConfig() {
        logger.info("Hazelcast HTTP Session management enabled");
        logger.info("Session timeout: 30 minutes");
        logger.info("Sessions will be stored in the Hazelcast cluster for distributed access");
    }
}
