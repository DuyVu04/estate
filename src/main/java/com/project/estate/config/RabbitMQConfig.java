package com.project.estate.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
  @Value("${rabbitmq.queue.email}")
  private String emailQueue;

  @Value("${rabbitmq.exchange.email}")
  private String emailExchange;

  @Value("${rabbitmq.routing-key.email}")
  private String emailRoutingKey;

  @Value("${rabbitmq.queue.dlq}")
  private String emailDlq;

  @Value("${rabbitmq.exchange.dlq}")
  private String deadLetterExchange;

  @Value("${rabbitmq.routing-key.dlq}")
  private String deadLetterRoutingKey;

  @Value("${rabbitmq.queue.retry}")
  private String retryQueue;

  @Value("${rabbitmq.exchange.retry}")
  private String retryExchange;

  @Value("${rabbitmq.routing-key.retry}")
  private String retryRoutingKey;

  @Value("${rabbitmq.exchange.property:property.exchange}")
  private String propertyExchange;

  @Value("${rabbitmq.queue.property-embedding:property.embedding.queue}")
  private String embeddingQueue;

  @Value("${rabbitmq.routing-key.property-embedding:property.embedding}")
  private String embeddingRoutingKey;

  @Bean
  public Queue emailQueue() {
    return QueueBuilder.durable(emailQueue).build();
  }

  @Bean
  public TopicExchange propertyExchange() {
    return new TopicExchange(propertyExchange, true, false);
  }

  @Bean
  public Queue embeddingQueue() {
    return QueueBuilder.durable(embeddingQueue).build();
  }

  @Bean
  public DirectExchange emailExchange() {
    return ExchangeBuilder.directExchange(emailExchange).durable(true).build();
  }

  @Bean
  public DirectExchange deadLetterExchange() {
    return ExchangeBuilder.directExchange(deadLetterExchange).durable(true).build();
  }

  @Bean
  public DirectExchange retryExchange() {
    return ExchangeBuilder.directExchange(retryExchange).durable(true).build();
  }

  @Bean
  public Queue deadLetterQueue() {
    return QueueBuilder.durable(emailDlq).build();
  }

  @Bean
  public Queue retryQueue() {
    return QueueBuilder.durable(retryQueue)
        // Sau 10 giây
        .ttl(10000)
        // Quay về email.exchange
        .deadLetterExchange(emailExchange)
        // Routing lại email.queue
        .deadLetterRoutingKey(emailRoutingKey)
        .build();
  }

  @Bean
  public Binding emailBinding(
      @Qualifier("emailQueue") Queue queue, @Qualifier("emailExchange") DirectExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(emailRoutingKey);
  }

  @Bean
  public Binding deadLetterBinding(
      @Qualifier("deadLetterQueue") Queue queue,
      @Qualifier("deadLetterExchange") DirectExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(deadLetterRoutingKey);
  }

  @Bean
  public Binding retryBinding(
      @Qualifier("retryQueue") Queue queue, @Qualifier("retryExchange") DirectExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(retryRoutingKey);
  }

  @Bean
  public Binding embeddingBinding(
      @Qualifier("embeddingQueue") Queue queue,
      @Qualifier("propertyExchange") TopicExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(embeddingRoutingKey);
  }

  @Bean
  public MessageConverter messageConverter() {
    return new JacksonJsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, MessageConverter messageConverter) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(messageConverter);
    return rabbitTemplate;
  }
}
