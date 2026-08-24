package com.project.estate.messaging.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyVectorProducer {

  private final RabbitTemplate rabbitTemplate;

  @Value("${rabbitmq.exchange.property:property.exchange}")
  private String exchange;

  @Value("${rabbitmq.routing-key.property-embedding:property.embedding}")
  private String routingKey;

  /**
   * Publishes an event to calculate vector embeddings for the created/updated property. Ensures the
   * message is sent ONLY AFTER the database transaction has committed.
   *
   * @param propertyId The ID of the property
   */
  public void publishPropertyEmbeddingTask(String propertyId) {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              log.info(
                  "[RABBITMQ_PRODUCER] Publishing async embedding calculation task (after commit) for propertyId={}",
                  propertyId);
              rabbitTemplate.convertAndSend(exchange, routingKey, propertyId);
            }
          });
    } else {
      log.info(
          "[RABBITMQ_PRODUCER] Publishing async embedding calculation task for propertyId={}",
          propertyId);
      rabbitTemplate.convertAndSend(exchange, routingKey, propertyId);
    }
  }
}
