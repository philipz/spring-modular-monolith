package com.sivalabs.bookstore.orders.web;

import com.sivalabs.bookstore.common.models.ErrorResponse;
import com.sivalabs.bookstore.orders.InvalidOrderException;
import com.sivalabs.bookstore.orders.OrderNotFoundException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = OrdersRestController.class)
public class OrdersRestExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(OrdersRestExceptionHandler.class);

    /**
     * Handle OrderNotFoundException - maps to HTTP 404
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle InvalidOrderException - maps to HTTP 400
     */
    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrder(InvalidOrderException ex) {
        log.warn("Invalid order: {}", ex.getMessage());
        ErrorResponse errorResponse =
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle gRPC StatusRuntimeException - maps to appropriate HTTP status codes
     */
    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGrpcException(StatusRuntimeException ex) {
        Status status = ex.getStatus();
        String description = status.getDescription() != null
                ? status.getDescription()
                : status.getCode().name();

        HttpStatus httpStatus;
        String message;

        switch (status.getCode()) {
            case NOT_FOUND -> {
                httpStatus = HttpStatus.NOT_FOUND;
                message = description;
                log.warn("gRPC NOT_FOUND: {}", description);
            }
            case INVALID_ARGUMENT -> {
                httpStatus = HttpStatus.BAD_REQUEST;
                message = description;
                log.warn("gRPC INVALID_ARGUMENT: {}", description);
            }
            case UNAVAILABLE -> {
                httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
                message = "Orders service unavailable. Please try again later.";
                log.error("gRPC UNAVAILABLE: {}", description);
            }
            case DEADLINE_EXCEEDED -> {
                httpStatus = HttpStatus.GATEWAY_TIMEOUT;
                message = "Request timeout. Please try again later.";
                log.error("gRPC DEADLINE_EXCEEDED: {}", description);
            }
            default -> {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                message = "Unable to process request at the moment. Please try again later.";
                log.error("gRPC error {}: {}", status.getCode(), description, ex);
            }
        }

        ErrorResponse errorResponse = new ErrorResponse(httpStatus.value(), message, LocalDateTime.now());
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    /**
     * Handle validation errors from @Valid - maps to HTTP 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        StringBuilder messageBuilder = new StringBuilder("Validation failed: ");

        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                messageBuilder
                        .append(fieldError.getField())
                        .append(" - ")
                        .append(fieldError.getDefaultMessage())
                        .append("; ");
            } else {
                messageBuilder.append(error.getDefaultMessage()).append("; ");
            }
        });

        String message = messageBuilder.toString();
        log.warn("Validation error: {}", message);

        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle ResponseStatusException - preserves the HTTP status from the exception
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("ResponseStatusException: {}", ex.getReason());
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getStatusCode().value(),
                ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString(),
                LocalDateTime.now());
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    /**
     * Handle generic exceptions - maps to HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error in orders REST API", ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred. Please try again later.",
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
