package com.sivalabs.bookstore.orders.support;

import org.testcontainers.DockerClientFactory;

/** Utility to determine whether Docker is reachable for Testcontainers-based tests. */
public final class DockerAvailability {

    private static final boolean DOCKER_AVAILABLE = detectDocker();

    private DockerAvailability() {}

    private static boolean detectDocker() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    public static boolean isDockerAvailable() {
        return DOCKER_AVAILABLE;
    }
}
