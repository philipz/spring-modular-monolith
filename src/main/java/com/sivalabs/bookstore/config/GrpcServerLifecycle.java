package com.sivalabs.bookstore.config;

import io.grpc.Server;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Manages the gRPC server lifecycle within the Spring application context.
 */
public class GrpcServerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private final Server server;
    private final long shutdownGracePeriodSeconds;

    private volatile boolean running;

    public GrpcServerLifecycle(Server server, long shutdownGracePeriodSeconds) {
        this.server = server;
        this.shutdownGracePeriodSeconds = shutdownGracePeriodSeconds;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        try {
            server.start();
            running = true;
            log.info("gRPC server started on port {}", server.getPort());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start gRPC server", ex);
        }
    }

    @Override
    public void stop() {
        stop(() -> {});
    }

    @Override
    public void stop(Runnable callback) {
        if (!running) {
            callback.run();
            return;
        }

        server.shutdown();
        try {
            if (!server.awaitTermination(shutdownGracePeriodSeconds, TimeUnit.SECONDS)) {
                log.warn(
                        "gRPC server did not shutdown gracefully within {} seconds. Forcing shutdown.",
                        shutdownGracePeriodSeconds);
                server.shutdownNow();
            } else {
                log.info("gRPC server stopped gracefully");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            server.shutdownNow();
        } finally {
            running = false;
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running && !server.isShutdown() && !server.isTerminated();
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
