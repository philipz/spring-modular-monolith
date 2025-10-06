package com.sivalabs.bookstore.orders.infrastructure.grpc;

import com.sivalabs.bookstore.orders.InvalidOrderException;
import com.sivalabs.bookstore.orders.OrderNotFoundException;
import com.sivalabs.bookstore.orders.infrastructure.catalog.CatalogServiceException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnClass(name = "io.grpc.Status")
public class GrpcExceptionMapper {

    public StatusRuntimeException map(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable must not be null");

        Throwable rootCause = unwrap(throwable);
        if (rootCause instanceof StatusRuntimeException statusRuntimeException) {
            return statusRuntimeException;
        }

        if (rootCause instanceof jakarta.validation.ConstraintViolationException violationException) {
            var violations = violationException.getConstraintViolations();
            String description = violations == null || violations.isEmpty()
                    ? null
                    : violations.stream()
                            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                            .collect(java.util.stream.Collectors.joining(", "));
            if (description == null || description.isBlank()) {
                description = violationException.getMessage();
                if (description == null || description.isBlank()) {
                    description = "Validation failed";
                }
            }
            return Status.INVALID_ARGUMENT
                    .withDescription(description)
                    .withCause(rootCause)
                    .asRuntimeException();
        }

        if (rootCause instanceof OrderNotFoundException) {
            return Status.NOT_FOUND
                    .withDescription(rootCause.getMessage())
                    .withCause(rootCause)
                    .asRuntimeException();
        }

        if (rootCause instanceof InvalidOrderException || rootCause instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT
                    .withDescription(rootCause.getMessage())
                    .withCause(rootCause)
                    .asRuntimeException();
        }

        if (rootCause instanceof CatalogServiceException) {
            String description = rootCause.getMessage();
            if (description == null || description.isBlank()) {
                description = "Catalog service is currently unavailable";
            }
            description = description + ". Please retry the request.";
            return Status.UNAVAILABLE
                    .withDescription(description)
                    .withCause(rootCause)
                    .asRuntimeException();
        }

        return Status.INTERNAL
                .withDescription("Unexpected gRPC error")
                .withCause(rootCause)
                .asRuntimeException();
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException || throwable instanceof ExecutionException) {
            Throwable cause = throwable.getCause();
            return cause == null ? throwable : unwrap(cause);
        }
        return throwable;
    }
}
