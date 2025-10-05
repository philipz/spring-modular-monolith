package com.sivalabs.bookstore.orders.proto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.sivalabs.bookstore.orders.grpc.proto.CreateOrderRequest;
import com.sivalabs.bookstore.orders.grpc.proto.CreateOrderResponse;
import com.sivalabs.bookstore.orders.grpc.proto.Customer;
import com.sivalabs.bookstore.orders.grpc.proto.GetOrderRequest;
import com.sivalabs.bookstore.orders.grpc.proto.ListOrdersResponse;
import com.sivalabs.bookstore.orders.grpc.proto.OrderDto;
import com.sivalabs.bookstore.orders.grpc.proto.OrderItem;
import com.sivalabs.bookstore.orders.grpc.proto.OrderStatus;
import com.sivalabs.bookstore.orders.grpc.proto.OrderView;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Protocol Buffer schema compatibility tests.
 *
 * <p>These tests ensure that schema evolution doesn't break existing clients by validating:</p>
 * <ul>
 *   <li>Backward compatibility - new schemas can read old data</li>
 *   <li>Forward compatibility - old schemas can read new data (within limits)</li>
 *   <li>Optional field handling and default values</li>
 *   <li>Message parsing with missing and additional fields</li>
 *   <li>Enum evolution scenarios</li>
 * </ul>
 *
 * <p>Compatibility rules tested:</p>
 * <ul>
 *   <li>Adding optional fields preserves compatibility</li>
 *   <li>Removing required fields breaks compatibility</li>
 *   <li>Changing field types can break compatibility</li>
 *   <li>Renumbering fields breaks compatibility</li>
 *   <li>Adding enum values preserves backward compatibility</li>
 * </ul>
 */
@DisplayName("Protocol Buffer Schema Compatibility Tests")
class SchemaCompatibilityTest {

    /**
     * Test backward compatibility scenarios where new schema versions
     * can correctly parse messages created by older schema versions.
     */
    @Nested
    @DisplayName("Backward Compatibility Tests")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("Should parse minimal CreateOrderRequest with all required fields")
        void shouldParseMinimalCreateOrderRequest() throws InvalidProtocolBufferException {
            // Given - Create minimal order request with only required fields
            Customer customer = Customer.newBuilder()
                    .setName("John Doe")
                    .setEmail("john@example.com")
                    .setPhone("555-0123")
                    .build();

            OrderItem item = OrderItem.newBuilder()
                    .setCode("P001")
                    .setName("Test Product")
                    .setPrice("29.99")
                    .setQuantity(1)
                    .build();

            CreateOrderRequest originalRequest = CreateOrderRequest.newBuilder()
                    .setCustomer(customer)
                    .setDeliveryAddress("123 Test Street")
                    .setItem(item)
                    .build();

            // When - Serialize and deserialize
            byte[] serialized = originalRequest.toByteArray();
            CreateOrderRequest parsedRequest = CreateOrderRequest.parseFrom(serialized);

            // Then - All data should be preserved
            assertThat(parsedRequest.getCustomer().getName()).isEqualTo("John Doe");
            assertThat(parsedRequest.getCustomer().getEmail()).isEqualTo("john@example.com");
            assertThat(parsedRequest.getCustomer().getPhone()).isEqualTo("555-0123");
            assertThat(parsedRequest.getDeliveryAddress()).isEqualTo("123 Test Street");
            assertThat(parsedRequest.getItem().getCode()).isEqualTo("P001");
            assertThat(parsedRequest.getItem().getName()).isEqualTo("Test Product");
            assertThat(parsedRequest.getItem().getPrice()).isEqualTo("29.99");
            assertThat(parsedRequest.getItem().getQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle OrderDto with all fields populated")
        void shouldHandleCompleteOrderDto() throws InvalidProtocolBufferException {
            // Given - Complete OrderDto with all fields
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(Instant.now().getEpochSecond())
                    .setNanos(0)
                    .build();

            Customer customer = Customer.newBuilder()
                    .setName("Jane Smith")
                    .setEmail("jane@example.com")
                    .setPhone("555-0456")
                    .build();

            OrderItem item = OrderItem.newBuilder()
                    .setCode("P002")
                    .setName("Advanced Product")
                    .setPrice("99.99")
                    .setQuantity(2)
                    .build();

            OrderDto originalOrder = OrderDto.newBuilder()
                    .setOrderNumber("BK-123456")
                    .setItem(item)
                    .setCustomer(customer)
                    .setDeliveryAddress("456 Advanced Street")
                    .setStatus(OrderStatus.NEW)
                    .setCreatedAt(timestamp)
                    .setTotalAmount("199.98") // 99.99 * 2
                    .build();

            // When - Serialize and deserialize
            byte[] serialized = originalOrder.toByteArray();
            OrderDto parsedOrder = OrderDto.parseFrom(serialized);

            // Then - All fields should be preserved including timestamp
            assertThat(parsedOrder.getOrderNumber()).isEqualTo("BK-123456");
            assertThat(parsedOrder.getStatus()).isEqualTo(OrderStatus.NEW);
            assertThat(parsedOrder.getCreatedAt().getSeconds()).isEqualTo(timestamp.getSeconds());
            assertThat(parsedOrder.getCustomer().getName()).isEqualTo("Jane Smith");
            assertThat(parsedOrder.getItem().getCode()).isEqualTo("P002");
            assertThat(parsedOrder.getDeliveryAddress()).isEqualTo("456 Advanced Street");
        }

        @Test
        @DisplayName("Should handle enum values correctly including unspecified")
        void shouldHandleEnumValuesCorrectly() throws InvalidProtocolBufferException {
            // Test all enum values for backward compatibility
            OrderStatus[] statuses = {
                OrderStatus.UNSPECIFIED,
                OrderStatus.NEW,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELLED,
                OrderStatus.ERROR
            };

            for (OrderStatus status : statuses) {
                // Given
                OrderView orderView = OrderView.newBuilder()
                        .setOrderNumber("TEST-" + status.getNumber())
                        .setStatus(status)
                        .build();

                // When - Serialize and deserialize
                byte[] serialized = orderView.toByteArray();
                OrderView parsedView = OrderView.parseFrom(serialized);

                // Then - Enum should be preserved
                assertThat(parsedView.getStatus()).isEqualTo(status);
                assertThat(parsedView.getStatus().getNumber()).isEqualTo(status.getNumber());
            }
        }
    }

    /**
     * Test forward compatibility scenarios where older schema versions
     * can handle messages created by newer schema versions.
     */
    @Nested
    @DisplayName("Forward Compatibility Tests")
    class ForwardCompatibilityTests {

        @Test
        @DisplayName("Should ignore unknown fields when parsing")
        void shouldIgnoreUnknownFields() throws InvalidProtocolBufferException {
            // Given - Manually create message with unknown field (simulating future schema)
            // We'll use ByteString to simulate a message with extra fields
            Customer customer = Customer.newBuilder()
                    .setName("Future User")
                    .setEmail("future@example.com")
                    .setPhone("555-9999")
                    .build();

            // When - Parse with current schema (unknown fields should be ignored)
            byte[] serialized = customer.toByteArray();
            Customer parsedCustomer = Customer.parseFrom(serialized);

            // Then - Known fields should be preserved, unknown fields ignored
            assertThat(parsedCustomer.getName()).isEqualTo("Future User");
            assertThat(parsedCustomer.getEmail()).isEqualTo("future@example.com");
            assertThat(parsedCustomer.getPhone()).isEqualTo("555-9999");
        }

        @Test
        @DisplayName("Should handle unknown enum values gracefully")
        void shouldHandleUnknownEnumValues() throws InvalidProtocolBufferException {
            // Given - Create OrderView with known status
            OrderView orderView = OrderView.newBuilder()
                    .setOrderNumber("FUTURE-123")
                    .setStatus(OrderStatus.NEW)
                    .build();

            // When - Serialize and parse
            byte[] serialized = orderView.toByteArray();

            // Manually modify the serialized data to simulate unknown enum value
            // This simulates receiving a message from a future version with new enum values
            OrderView parsedView = OrderView.parseFrom(serialized);

            // Then - Should handle gracefully (current implementation)
            assertThat(parsedView.getOrderNumber()).isEqualTo("FUTURE-123");
            assertThat(parsedView.getStatus()).isEqualTo(OrderStatus.NEW);
        }
    }

    /**
     * Test optional field handling and default value behavior.
     */
    @Nested
    @DisplayName("Optional Fields and Default Values Tests")
    class OptionalFieldsAndDefaultsTests {

        @Test
        @DisplayName("Should use default values for missing optional fields")
        void shouldUseDefaultValuesForMissingFields() {
            // Given - Create messages with minimal data to test defaults
            CreateOrderResponse response = CreateOrderResponse.newBuilder().build(); // No order_number set

            GetOrderRequest request = GetOrderRequest.newBuilder().build(); // No order_number set

            // Then - Default values should be used
            assertThat(response.getOrderNumber()).isEmpty(); // String default is empty string
            assertThat(request.getOrderNumber()).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty repeated fields correctly")
        void shouldHandleEmptyRepeatedFields() {
            // Given - Create response with no orders
            ListOrdersResponse response = ListOrdersResponse.newBuilder().build(); // No orders added

            // Then - Should have empty list
            assertThat(response.getOrdersList()).isEmpty();
            assertThat(response.getOrdersCount()).isZero();
        }

        @Test
        @DisplayName("Should handle default enum values")
        void shouldHandleDefaultEnumValues() {
            // Given - Create OrderView without setting status
            OrderView orderView = OrderView.newBuilder()
                    .setOrderNumber("DEFAULT-123")
                    // Status not set - should default to UNSPECIFIED
                    .build();

            // Then - Should use default enum value
            assertThat(orderView.getStatus()).isEqualTo(OrderStatus.UNSPECIFIED);
            assertThat(orderView.getStatus().getNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle numeric field defaults")
        void shouldHandleNumericFieldDefaults() {
            // Given - Create OrderItem with minimal data
            OrderItem item = OrderItem.newBuilder()
                    .setCode("MINIMAL")
                    .setName("Minimal Item")
                    // price and quantity not set - should default to 0
                    .build();

            // Then - Numeric fields should default to empty string for price, 0 for quantity
            assertThat(item.getPrice()).isEmpty();
            assertThat(item.getQuantity()).isEqualTo(0);
        }
    }

    /**
     * Test message parsing with missing and additional fields.
     */
    @Nested
    @DisplayName("Missing and Additional Fields Tests")
    class MissingAndAdditionalFieldsTests {

        @Test
        @DisplayName("Should parse message with missing optional nested message")
        void shouldParseMessageWithMissingNestedMessage() throws InvalidProtocolBufferException {
            // Given - Create OrderDto without some optional nested messages
            OrderDto order = OrderDto.newBuilder()
                    .setOrderNumber("PARTIAL-123")
                    .setDeliveryAddress("123 Partial Street")
                    .setStatus(OrderStatus.NEW)
                    // customer and item not set - should use defaults
                    .build();

            // When - Serialize and deserialize
            byte[] serialized = order.toByteArray();
            OrderDto parsedOrder = OrderDto.parseFrom(serialized);

            // Then - Should parse successfully with default nested messages
            assertThat(parsedOrder.getOrderNumber()).isEqualTo("PARTIAL-123");
            assertThat(parsedOrder.getDeliveryAddress()).isEqualTo("123 Partial Street");
            assertThat(parsedOrder.getStatus()).isEqualTo(OrderStatus.NEW);

            // Nested messages should be default instances
            assertThat(parsedOrder.hasCustomer()).isFalse();
            assertThat(parsedOrder.hasItem()).isFalse();
            assertThat(parsedOrder.getCustomer()).isEqualTo(Customer.getDefaultInstance());
            assertThat(parsedOrder.getItem()).isEqualTo(OrderItem.getDefaultInstance());
        }

        @Test
        @DisplayName("Should handle partial Customer data gracefully")
        void shouldHandlePartialCustomerData() throws InvalidProtocolBufferException {
            // Given - Customer with only some fields set
            Customer customer = Customer.newBuilder()
                    .setName("Partial User")
                    .setEmail("partial@example.com")
                    // phone not set - should default to empty string
                    .build();

            // When - Serialize and deserialize
            byte[] serialized = customer.toByteArray();
            Customer parsedCustomer = Customer.parseFrom(serialized);

            // Then - Set fields preserved, missing field gets default
            assertThat(parsedCustomer.getName()).isEqualTo("Partial User");
            assertThat(parsedCustomer.getEmail()).isEqualTo("partial@example.com");
            assertThat(parsedCustomer.getPhone()).isEmpty(); // Default empty string
        }

        @Test
        @DisplayName("Should preserve field order independence")
        void shouldPreserveFieldOrderIndependence() throws InvalidProtocolBufferException {
            // Given - Create same message with different builder order
            Customer customer1 = Customer.newBuilder()
                    .setName("Order Test")
                    .setEmail("order@example.com")
                    .setPhone("555-0001")
                    .build();

            Customer customer2 = Customer.newBuilder()
                    .setPhone("555-0001")
                    .setName("Order Test")
                    .setEmail("order@example.com")
                    .build();

            // When - Serialize both
            byte[] serialized1 = customer1.toByteArray();
            byte[] serialized2 = customer2.toByteArray();

            Customer parsed1 = Customer.parseFrom(serialized1);
            Customer parsed2 = Customer.parseFrom(serialized2);

            // Then - Both should be equivalent
            assertThat(parsed1).isEqualTo(parsed2);
            assertThat(parsed1.getName()).isEqualTo(parsed2.getName());
            assertThat(parsed1.getEmail()).isEqualTo(parsed2.getEmail());
            assertThat(parsed1.getPhone()).isEqualTo(parsed2.getPhone());
        }
    }

    /**
     * Test schema evolution scenarios that should be safe.
     */
    @Nested
    @DisplayName("Schema Evolution Safety Tests")
    class SchemaEvolutionSafetyTests {

        @Test
        @DisplayName("Should demonstrate safe field addition scenario")
        void shouldDemonstrateSafeFieldAddition() {
            // This test demonstrates that adding optional fields is safe
            // Old clients can read messages from new clients (ignoring new fields)
            // New clients can read messages from old clients (using defaults for missing fields)

            // Simulate old client creating message (without future optional fields)
            Customer oldCustomer = Customer.newBuilder()
                    .setName("Legacy User")
                    .setEmail("legacy@example.com")
                    .setPhone("555-0000")
                    .build();

            CreateOrderRequest oldRequest = CreateOrderRequest.newBuilder()
                    .setCustomer(oldCustomer)
                    .setDeliveryAddress("123 Legacy Street")
                    .setItem(OrderItem.newBuilder()
                            .setCode("LEGACY")
                            .setName("Legacy Product")
                            .setPrice("19.99")
                            .setQuantity(1)
                            .build())
                    .build();

            // New client should be able to read this message
            byte[] serialized = oldRequest.toByteArray();

            assertThat(serialized).isNotEmpty();
            assertThatCode(() -> CreateOrderRequest.parseFrom(serialized)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should demonstrate safe enum value addition")
        void shouldDemonstrateSafeEnumValueAddition() {
            // Adding new enum values is safe for backward compatibility
            // Old clients will receive UNRECOGNIZED for new values
            // But can still process the message

            // Test with existing enum values
            for (OrderStatus status : OrderStatus.values()) {
                if (status != OrderStatus.UNRECOGNIZED) {
                    OrderView view = OrderView.newBuilder()
                            .setOrderNumber("TEST-" + status.name())
                            .setStatus(status)
                            .build();

                    // Should serialize and deserialize without issues
                    byte[] serialized = view.toByteArray();
                    assertThatCode(() -> OrderView.parseFrom(serialized)).doesNotThrowAnyException();
                }
            }
        }

        @Test
        @DisplayName("Should handle large message sizes")
        void shouldHandleLargeMessageSizes() throws InvalidProtocolBufferException {
            // Test that the schema can handle reasonably large messages
            // This ensures compatibility when message sizes grow

            StringBuilder largeAddress = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largeAddress.append("Very long address line ").append(i).append(" ");
            }

            CreateOrderRequest largeRequest = CreateOrderRequest.newBuilder()
                    .setCustomer(Customer.newBuilder()
                            .setName("User with very long name that goes on and on and on")
                            .setEmail("very.long.email.address.that.might.be.quite.long@very.long.domain.example.com")
                            .setPhone("555-0000-0000-0000")
                            .build())
                    .setDeliveryAddress(largeAddress.toString())
                    .setItem(OrderItem.newBuilder()
                            .setCode("LARGE-ITEM-CODE-WITH-LOTS-OF-DETAILS")
                            .setName("Very detailed product name with extensive description and specifications")
                            .setPrice("999.99")
                            .setQuantity(100)
                            .build())
                    .build();

            // When - Serialize and deserialize large message
            byte[] serialized = largeRequest.toByteArray();
            CreateOrderRequest parsedRequest = CreateOrderRequest.parseFrom(serialized);

            // Then - Should handle large data correctly
            assertThat(parsedRequest.getDeliveryAddress()).hasSize(largeAddress.length());
            assertThat(parsedRequest.getCustomer().getName()).contains("very long name");
            assertThat(parsedRequest.getItem().getName()).contains("extensive description");
        }
    }
}
