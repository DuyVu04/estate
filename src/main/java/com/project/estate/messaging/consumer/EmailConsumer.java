package com.project.estate.messaging.consumer;

import com.project.estate.messaging.dto.EmailVerificationMessage;
import com.project.estate.service.email.JavaMailEmailService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {
    private final JavaMailEmailService mailService;

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.retry}")
    private String retryExchange;

    @Value("${rabbitmq.routing-key.retry}")
    private String retryRoutingKey;

    @Value("${rabbitmq.exchange.dlq}")
    private String deadExchange;

    @Value("${rabbitmq.routing-key.dlq}")
    private String deadRoutingKey;

    @RabbitListener(queues = "${rabbitmq.queue.email}")
    public void receive(EmailVerificationMessage message,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag)  {

        log.info("Received verification message: {}", message);
        log.info(
                "Receive email={}, retryCount={}",
                message.email(),
                message.retryCount()
        );
        try {

            mailService.sendConfirmLink(message.email(), message.token());

            channel.basicAck(tag, false);

        } catch (Exception e) {

            try {

                if (message.retryCount() >= 3) {

                    rabbitTemplate.convertAndSend(
                            deadExchange,
                            deadRoutingKey,
                            message
                    );
                    log.error("Publish to DLQ: email={}, retryCount={}", message.email(), message.retryCount());

                } else {

                    rabbitTemplate.convertAndSend(
                            retryExchange,
                            retryRoutingKey,
                            message.nextRetry()
                    );

                }
                // Chỉ ACK khi publish thành công
                channel.basicAck(tag, false);

            } catch (Exception ex) {
                log.error("Publish retry/DLQ failed", ex);

                // Không ACK
                // RabbitMQ sẽ redeliver message gốc
            }
        }

    }

}
