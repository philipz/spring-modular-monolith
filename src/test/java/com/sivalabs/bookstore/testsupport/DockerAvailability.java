package com.sivalabs.bookstore.testsupport;

import org.testcontainers.DockerClientFactory;

/** Utility to detect whether Docker is available for Testcontainers-based tests. */
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
