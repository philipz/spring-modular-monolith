package com.sivalabs.bookstore.testsupport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Meta-annotation to enable tests only when Docker is reachable.
 *
 * Useful for Testcontainers-based integration tests so they are skipped automatically
 * in environments without Docker support.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnabledIf("com.sivalabs.bookstore.testsupport.DockerAvailability#isDockerAvailable")
public @interface EnabledIfDockerAvailable {}
