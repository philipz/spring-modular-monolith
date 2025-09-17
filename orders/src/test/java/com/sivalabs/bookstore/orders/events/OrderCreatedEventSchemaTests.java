package com.sivalabs.bookstore.orders.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivalabs.bookstore.orders.api.events.OrderCreatedEvent;
import com.sivalabs.bookstore.orders.api.model.Customer;
import com.sivalabs.bookstore.orders.config.RabbitMQConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.events.Externalized;

class OrderCreatedEventSchemaTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("OrderCreatedEvent should declare expected exchange and routing")
    void orderCreatedEventShouldDeclareExpectedRouting() {
        Externalized externalized = OrderCreatedEvent.class.getAnnotation(Externalized.class);
        assertThat(externalized).as("@Externalized must be present").isNotNull();
        assertThat(externalized.value()).isEqualTo(RabbitMQConfig.EXCHANGE_NAME + "::" + RabbitMQConfig.ROUTING_KEY);
    }

    @Test
    @DisplayName("OrderCreatedEvent JSON structure should expose contract fields")
    void orderCreatedEventJsonStructureMatchesContract() throws Exception {
        Customer customer = new Customer("John Doe", "john@example.com", "123-456-7890");
        OrderCreatedEvent event = new OrderCreatedEvent("ORD-999", "PROD-42", 2, customer);

        JsonNode payload = objectMapper.readTree(objectMapper.writeValueAsString(event));

        assertThat(payload.get("orderNumber").asText()).isEqualTo("ORD-999");
        assertThat(payload.get("productCode").asText()).isEqualTo("PROD-42");
        assertThat(payload.get("quantity").asInt()).isEqualTo(2);

        JsonNode customerNode = payload.get("customer");
        assertThat(customerNode).isNotNull();
        assertThat(customerNode.get("name").asText()).isEqualTo(customer.name());
        assertThat(customerNode.get("email").asText()).isEqualTo(customer.email());
        assertThat(customerNode.get("phone").asText()).isEqualTo(customer.phone());
    }
}
