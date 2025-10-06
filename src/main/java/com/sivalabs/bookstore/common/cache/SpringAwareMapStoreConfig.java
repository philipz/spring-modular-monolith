package com.sivalabs.bookstore.common.cache;

import com.hazelcast.config.MapStoreConfig;

/**
 * MapStoreConfig that exposes the MapStore's class name even when a Spring-managed
 * implementation instance is provided directly.
 *
 * <p>Hazelcast's default MapStoreConfig#setImplementation(Object) clears the class name,
 * which causes getClassName() to return {@code null}. Some of our integration tests expect
 * the fully qualified class name to be available for verification while we still rely on
 * Spring-managed MapStore beans. This subclass bridges that gap by deriving the class name
 * from the provided implementation when necessary.</p>
 */
public class SpringAwareMapStoreConfig extends MapStoreConfig {

    @Override
    public String getClassName() {
        String className = super.getClassName();
        if (className == null) {
            Object implementation = getImplementation();
            if (implementation != null) {
                return implementation.getClass().getName();
            }
        }
        return className;
    }
}
