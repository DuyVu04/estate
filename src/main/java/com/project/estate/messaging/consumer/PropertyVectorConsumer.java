package com.project.estate.messaging.consumer;

import com.project.estate.entity.Property;
import com.project.estate.repository.PropertyRepository;
import com.project.estate.service.GeminiEmbeddingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyVectorConsumer {

  private final PropertyRepository propertyRepository;
  private final GeminiEmbeddingService geminiEmbeddingService;

  @RabbitListener(queues = "${rabbitmq.queue.property-embedding:property.embedding.queue}")
  public void handlePropertyEmbeddingTask(String propertyId) {
    log.info("[RABBITMQ_CONSUMER] Received embedding task for propertyId={}", propertyId);

    try {
      Property property = propertyRepository.findById(propertyId).orElse(null);
      if (property == null) {
        log.warn("[RABBITMQ_CONSUMER] Property not found for id={}", propertyId);
        return;
      }

      String richText =
          String.format(
              "%s. %s. Loại hình: %s. Địa chỉ: %s, %s, %s, %s. Giá: %,.0f VNĐ. Diện tích: %.1f m2.",
              property.getTitle(),
              property.getDescription(),
              property.getPropertyType(),
              property.getAddress(),
              property.getWard(),
              property.getDistrict(),
              property.getCity(),
              property.getPrice(),
              property.getArea());

      List<Double> vector = geminiEmbeddingService.generateEmbedding(richText);
      String vectorStr = geminiEmbeddingService.toVectorString(vector);

      if (vectorStr != null) {
        propertyRepository.updateEmbedding(propertyId, vectorStr);
        log.info(
            "[RABBITMQ_CONSUMER] Successfully updated 768-dim embedding for propertyId={}",
            propertyId);
      }
    } catch (Exception e) {
      log.error(
          "[RABBITMQ_CONSUMER] Error processing embedding task for propertyId={}: {}",
          propertyId,
          e.getMessage());
    }
  }
}
