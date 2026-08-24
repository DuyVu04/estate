package com.project.estate.service;

import com.project.estate.dto.response.PropertySemanticResponse;
import com.project.estate.entity.Property;
import com.project.estate.repository.PropertyRepository;
import com.project.estate.repository.projection.PropertySemanticProjection;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyAiService {

  private final PropertyRepository propertyRepository;
  private final GeminiEmbeddingService geminiEmbeddingService;

  public int syncAllPropertyEmbeddings() {
    List<Property> properties = propertyRepository.findAll();
    int count = 0;

    for (Property p : properties) {
      String richText =
          String.format(
              "%s. %s. Loại hình: %s. Địa chỉ: %s, %s, %s, %s. Giá: %,.0f VNĐ. Diện tích: %.1f m2.",
              p.getTitle(),
              p.getDescription(),
              p.getPropertyType(),
              p.getAddress(),
              p.getWard(),
              p.getDistrict(),
              p.getCity(),
              p.getPrice(),
              p.getArea());

      List<Double> vector = geminiEmbeddingService.generateEmbedding(richText);
      String vectorStr = geminiEmbeddingService.toVectorString(vector);

      if (vectorStr != null) {
        propertyRepository.updateEmbedding(p.getId(), vectorStr);
        count++;
        log.info(
            "[AI_SYNC] Synced embedding for propertyId={}, title='{}'", p.getId(), p.getTitle());
      }
    }
    return count;
  }

  public List<PropertySemanticResponse> searchSemantic(String query, int limit) {
    List<Double> queryVector = geminiEmbeddingService.generateEmbedding(query);
    String queryVectorStr = geminiEmbeddingService.toVectorString(queryVector);

    if (queryVectorStr == null) {
      return List.of();
    }

    List<PropertySemanticProjection> projections =
        propertyRepository.searchSemantic(queryVectorStr, limit);
    return mapProjectionsToResponses(projections);
  }

  public List<PropertySemanticResponse> searchHybrid(
      String query, String city, BigDecimal minPrice, BigDecimal maxPrice, int limit) {
    List<Double> queryVector = geminiEmbeddingService.generateEmbedding(query);
    String queryVectorStr = geminiEmbeddingService.toVectorString(queryVector);

    if (queryVectorStr == null) {
      return List.of();
    }

    List<PropertySemanticProjection> projections =
        propertyRepository.searchHybrid(queryVectorStr, city, minPrice, maxPrice, limit);
    return mapProjectionsToResponses(projections);
  }

  private List<PropertySemanticResponse> mapProjectionsToResponses(
      List<PropertySemanticProjection> projections) {
    List<PropertySemanticResponse> responses = new ArrayList<>();
    for (PropertySemanticProjection p : projections) {
      double score = p.getSimilarityScore() != null ? p.getSimilarityScore() : 0.0;
      responses.add(
          new PropertySemanticResponse(
              p.getId(),
              p.getTitle(),
              p.getDescription(),
              p.getPropertyType(),
              p.getAddress(),
              p.getWard(),
              p.getDistrict(),
              p.getCity(),
              p.getArea(),
              p.getPrice(),
              p.getStatus(),
              p.getCreatedAt(),
              p.getUpdatedAt(),
              score,
              Math.round(score * 1000.0) / 10.0));
    }
    return responses;
  }
}
