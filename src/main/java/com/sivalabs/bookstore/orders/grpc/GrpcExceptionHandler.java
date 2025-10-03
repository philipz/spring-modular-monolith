package com.sivalabs.bookstore.orders.grpc;

import com.sivalabs.bookstore.orders.InvalidOrderException;
import com.sivalabs.bookstore.orders.OrderNotFoundException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to translate domain/service exceptions into gRPC {@link StatusRuntimeException}.
 *
 * Detailed exception mapping will be implemented in follow-up tasks.
 */
public final class GrpcExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GrpcExceptionHandler.class);

    private GrpcExceptionHandler() {
        // utility class
    }

    public static StatusRuntimeException handleException(Exception exception) {
        if (exception instanceof OrderNotFoundException notFoundException) {
            log.debug("Order not found: {}", notFoundException.getMessage());
            return Status.NOT_FOUND
                    .withDescription(notFoundException.getMessage())
                    .withCause(notFoundException)
                    .asRuntimeException();
        }

        if (exception instanceof InvalidOrderException invalidOrderException) {
            log.debug("Invalid order: {}", invalidOrderException.getMessage());
            return Status.INVALID_ARGUMENT
                    .withDescription(invalidOrderException.getMessage())
                    .withCause(invalidOrderException)
                    .asRuntimeException();
        }

        if (exception instanceof ConstraintViolationException violationException) {
            log.debug("Constraint violation while processing gRPC request", violationException);
            String description = violationException.getConstraintViolations().stream()
                    .map(GrpcExceptionHandler::formatViolation)
                    .collect(Collectors.joining(", "));
            if (description.isBlank()) {
                description = violationException.getMessage();
            }
            return Status.INVALID_ARGUMENT
                    .withDescription(description)
                    .withCause(violationException)
                    .asRuntimeException();
        }

        log.error("Unhandled gRPC exception", exception);
        return Status.INTERNAL
                .withDescription(exception.getMessage() == null ? "Internal server error" : exception.getMessage())
                .withCause(exception)
                .asRuntimeException();
    }

    private static String formatViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }
}
