package com.project.estate.rabbitmq;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.project.estate.messaging.consumer.EmailConsumer;
import com.project.estate.messaging.dto.DepositPaidMessage;
import com.project.estate.messaging.producer.EmailProducer;
import com.project.estate.service.email.EmailTemplateService;
import com.rabbitmq.client.Channel;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class RabbitMQMessagingTest {

  @Mock private RabbitTemplate rabbitTemplate;

  @Mock private EmailTemplateService emailTemplateService;

  @Mock private Channel channel;

  @InjectMocks private EmailProducer producer;

  @InjectMocks private EmailConsumer consumer;

  @Test
  @DisplayName("Producer: Should send deposit paid message to RabbitMQ exchange")
  void producer_ShouldSendDepositPaidMessage() {
    DepositPaidMessage msg =
        new DepositPaidMessage(
            "res-100",
            "test@example.com",
            "Luxury Apartment",
            BigDecimal.valueOf(50000000),
            "TXN-123");

    producer.sendDepositPaid(msg);

    verify(rabbitTemplate, times(1)).convertAndSend(any(), any(), eq(msg));
  }

  @Test
  @DisplayName("Consumer: Should consume deposit paid message, send email and ACK channel")
  void consumer_ShouldConsumeDepositPaidAndAck() throws Exception {
    DepositPaidMessage msg =
        new DepositPaidMessage(
            "res-100",
            "test@example.com",
            "Luxury Apartment",
            BigDecimal.valueOf(50000000),
            "TXN-123");

    consumer.receiveDepositPaid(msg, channel, 1L);

    verify(emailTemplateService, times(1))
        .sendDepositPaidEmail(
            eq("test@example.com"), eq("res-100"), eq("Luxury Apartment"), any(), eq("TXN-123"));
    verify(channel, times(1)).basicAck(1L, false);
  }
}
