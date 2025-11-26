package com.sivalabs.bookstore.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "BookStoreExchange";

    public static final String ROUTING_KEY = "orders.new";

    public static final String QUEUE_NAME = "new-orders";

    public static final String DLX_NAME = "BookStoreDLX";

    public static final String DLQ_NAME = "new-orders-dlq";

    public static final String DLQ_ROUTING_KEY = "orders.new.dlq";

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    @ConditionalOnProperty(name = "app.amqp.new-orders.bind", havingValue = "true")
    Queue newOrdersQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.amqp.new-orders.bind", havingValue = "true")
    Binding newOrdersQueueBinding(Queue newOrdersQueue, TopicExchange exchange) {
        return BindingBuilder.bind(newOrdersQueue).to(exchange).with(ROUTING_KEY);
    }

    // Dead-letter exchange and queue configuration
    @Bean
    @ConditionalOnProperty(name = "app.amqp.new-orders.bind", havingValue = "true")
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME, true, false);
    }

    @Bean
    @ConditionalOnProperty(name = "app.amqp.new-orders.bind", havingValue = "true")
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.amqp.new-orders.bind", havingValue = "true")
    Binding deadLetterQueueBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final var rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(producerJackson2MessageConverter());
        return rabbitTemplate;
    }

    @Bean
    Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
