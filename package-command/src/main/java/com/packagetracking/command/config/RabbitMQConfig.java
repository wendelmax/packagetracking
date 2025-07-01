package com.packagetracking.command.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.modules.tracking-producer-enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    public static final String TRACKING_EVENTS_EXCHANGE = "tracking.events.exchange";
    
    public static final String TRACKING_EVENTS_QUEUE = "tracking.events.queue";
    
    public static final String TRACKING_EVENTS_RETRY_QUEUE = "tracking.events.retry.queue";
    
    public static final String TRACKING_EVENTS_DLQ = "tracking.events.dlq";

    public RabbitMQConfig() {
        System.out.println("Configuração RabbitMQ habilitada (automaticamente ativada pelo producer/consumer de tracking)");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        
        rabbitTemplate.setRetryTemplate(null);
        rabbitTemplate.setMandatory(true);
        
        return rabbitTemplate;
    }

    @Bean
    public DirectExchange trackingEventsExchange() {
        return new DirectExchange(TRACKING_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue trackingEventsQueue() {
        return QueueBuilder.durable(TRACKING_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", TRACKING_EVENTS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "tracking.events.retry")
                .build();
    }

    @Bean
    public Queue trackingEventsRetryQueue() {
        return QueueBuilder.durable(TRACKING_EVENTS_RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", TRACKING_EVENTS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "tracking.events.dlq")
                .withArgument("x-message-ttl", 30000) // 30 segundos
                .build();
    }

    @Bean
    public Queue trackingEventsDLQ() {
        return QueueBuilder.durable(TRACKING_EVENTS_DLQ).build();
    }

    @Bean
    public Binding trackingEventsBinding() {
        return BindingBuilder.bind(trackingEventsQueue())
                .to(trackingEventsExchange())
                .with("tracking.events");
    }

    @Bean
    public Binding trackingEventsRetryBinding() {
        return BindingBuilder.bind(trackingEventsRetryQueue())
                .to(trackingEventsExchange())
                .with("tracking.events.retry");
    }

    @Bean
    public Binding trackingEventsDLQBinding() {
        return BindingBuilder.bind(trackingEventsDLQ())
                .to(trackingEventsExchange())
                .with("tracking.events.dlq");
    }
} 