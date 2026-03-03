package com.smartmobility.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")              private String exchangeName;

    @Value("${rabbitmq.queue.trip-started}")         private String tripStartedQueue;
    @Value("${rabbitmq.queue.trip-completed}")       private String tripCompletedQueue;
    @Value("${rabbitmq.queue.pricing-fallback}")     private String pricingFallbackQueue;
    @Value("${rabbitmq.queue.low-balance}")          private String lowBalanceQueue;
    @Value("${rabbitmq.queue.insufficient-balance}") private String insufficientBalanceQueue;
    @Value("${rabbitmq.queue.daily-limit}")          private String dailyLimitQueue;
    @Value("${rabbitmq.queue.recharge-refused}")     private String rechargeRefusedQueue;
    @Value("${rabbitmq.queue.pass-suspended}")       private String passSuspendedQueue;
    @Value("${rabbitmq.queue.pass-activated}")       private String passActivatedQueue;

    @Value("${rabbitmq.routing-key.trip-started}")         private String tripStartedKey;
    @Value("${rabbitmq.routing-key.trip-completed}")       private String tripCompletedKey;
    @Value("${rabbitmq.routing-key.pricing-fallback}")     private String pricingFallbackKey;
    @Value("${rabbitmq.routing-key.low-balance}")          private String lowBalanceKey;
    @Value("${rabbitmq.routing-key.insufficient-balance}") private String insufficientBalanceKey;
    @Value("${rabbitmq.routing-key.daily-limit}")          private String dailyLimitKey;
    @Value("${rabbitmq.routing-key.recharge-refused}")     private String rechargeRefusedKey;
    @Value("${rabbitmq.routing-key.pass-suspended}")       private String passSuspendedKey;
    @Value("${rabbitmq.routing-key.pass-activated}")       private String passActivatedKey;

    @Bean public TopicExchange smartMobilityExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    // ── Queues ────────────────────────────────────────────────────────────────
    @Bean public Queue tripStartedQueue()         { return QueueBuilder.durable(tripStartedQueue).build(); }
    @Bean public Queue tripCompletedQueue()       { return QueueBuilder.durable(tripCompletedQueue).build(); }
    @Bean public Queue pricingFallbackQueue()     { return QueueBuilder.durable(pricingFallbackQueue).build(); }
    @Bean public Queue lowBalanceQueue()          { return QueueBuilder.durable(lowBalanceQueue).build(); }
    @Bean public Queue insufficientBalanceQueue() { return QueueBuilder.durable(insufficientBalanceQueue).build(); }
    @Bean public Queue dailyLimitQueue()          { return QueueBuilder.durable(dailyLimitQueue).build(); }
    @Bean public Queue rechargeRefusedQueue()     { return QueueBuilder.durable(rechargeRefusedQueue).build(); }
    @Bean public Queue passSuspendedQueue()       { return QueueBuilder.durable(passSuspendedQueue).build(); }
    @Bean public Queue passActivatedQueue()       { return QueueBuilder.durable(passActivatedQueue).build(); }

    // ── Bindings ──────────────────────────────────────────────────────────────
    @Bean public Binding tripStartedBinding()         { return bind(tripStartedQueue(),         tripStartedKey); }
    @Bean public Binding tripCompletedBinding()       { return bind(tripCompletedQueue(),       tripCompletedKey); }
    @Bean public Binding pricingFallbackBinding()     { return bind(pricingFallbackQueue(),     pricingFallbackKey); }
    @Bean public Binding lowBalanceBinding()          { return bind(lowBalanceQueue(),          lowBalanceKey); }
    @Bean public Binding insufficientBalanceBinding() { return bind(insufficientBalanceQueue(), insufficientBalanceKey); }
    @Bean public Binding dailyLimitBinding()          { return bind(dailyLimitQueue(),          dailyLimitKey); }
    @Bean public Binding rechargeRefusedBinding()     { return bind(rechargeRefusedQueue(),     rechargeRefusedKey); }
    @Bean public Binding passSuspendedBinding()       { return bind(passSuspendedQueue(),       passSuspendedKey); }
    @Bean public Binding passActivatedBinding()       { return bind(passActivatedQueue(),       passActivatedKey); }

    private Binding bind(Queue q, String key) {
        return BindingBuilder.bind(q).to(smartMobilityExchange()).with(key);
    }

    @Bean public MessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(messageConverter());
        return t;
    }
}