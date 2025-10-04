package com.sivalabs.bookstore.orders.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivalabs.bookstore.orders.InvalidOrderException;
import com.sivalabs.bookstore.orders.OrderNotFoundException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GrpcExceptionHandlerTest {

    @Test
    void shouldMapOrderNotFoundExceptionToNotFoundStatus() {
        StatusRuntimeException exception =
                GrpcExceptionHandler.handleException(new OrderNotFoundException("Order missing"));

        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(exception.getStatus().getDescription()).isEqualTo("Order missing");
    }

    @Test
    void shouldMapInvalidOrderExceptionToInvalidArgumentStatus() {
        StatusRuntimeException exception =
                GrpcExceptionHandler.handleException(new InvalidOrderException("Invalid order"));

        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(exception.getStatus().getDescription()).isEqualTo("Invalid order");
    }

    @Test
    void shouldMapConstraintViolationExceptionWithDetails() {
        ConstraintViolation<Object> violation = new TestConstraintViolation("customer.email", "must be a valid email");
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(Set.of(violation));

        StatusRuntimeException exception = GrpcExceptionHandler.handleException(constraintViolationException);

        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(exception.getStatus().getDescription()).contains("customer.email: must be a valid email");
    }

    @Test
    void shouldFallbackToInternalServerError() {
        StatusRuntimeException exception = GrpcExceptionHandler.handleException(new RuntimeException("boom"));

        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(exception.getStatus().getDescription()).isEqualTo("boom");
    }

    private static final class TestConstraintViolation implements ConstraintViolation<Object> {

        private final String propertyPath;
        private final String message;

        private TestConstraintViolation(String propertyPath, String message) {
            this.propertyPath = propertyPath;
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public String getMessageTemplate() {
            return null;
        }

        @Override
        public Object getRootBean() {
            return null;
        }

        @Override
        public Class<Object> getRootBeanClass() {
            return Object.class;
        }

        @Override
        public Object getLeafBean() {
            return null;
        }

        @Override
        public Object[] getExecutableParameters() {
            return new Object[0];
        }

        @Override
        public Object getExecutableReturnValue() {
            return null;
        }

        @Override
        public Path getPropertyPath() {
            return new SimplePath(propertyPath);
        }

        @Override
        public Object getInvalidValue() {
            return null;
        }

        @Override
        public ConstraintDescriptor<?> getConstraintDescriptor() {
            return null;
        }

        @Override
        public <U> U unwrap(Class<U> type) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class SimplePath implements Path {

        private final String name;

        private SimplePath(String name) {
            this.name = name;
        }

        @Override
        public Iterator<Node> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
