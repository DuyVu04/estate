package com.project.estate.messaging.producer;

import com.project.estate.messaging.dto.DepositPaidMessage;
import com.project.estate.messaging.dto.EmailVerificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.email}")
    private String exchange;

    @Value("${rabbitmq.routing-key.email}")
    private String routingKey;

    public void send(EmailVerificationMessage message) {

        log.info("Sending verification message: {}", message);

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                message
        );

    }

    public void sendDepositPaid(DepositPaidMessage message) {

        log.info("Sending deposit paid message: {}", message);

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                message
        );

    }
}