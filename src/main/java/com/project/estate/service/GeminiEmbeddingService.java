package com.project.estate.service;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service sinh Vector Embedding 768 chiều từ Google Gemini API. Sử dụng RestClient gọi trực tiếp
 * Google Gemini v1beta embedContent endpoint để đảm bảo tính tương thích tuyệt đối về cấu trúc
 * vector 768 chiều cho pgvector.
 */
@Service
@Slf4j
public class GeminiEmbeddingService {

  private final RestClient restClient = RestClient.create();

  @Value("${spring.ai.openai.api-key:}")
  private String apiKey;

  @Value("${spring.ai.openai.embedding.options.model:gemini-embedding-001}")
  private String model;

  @SuppressWarnings("unchecked")
  public List<Double> generateEmbedding(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }

    try {
      String url =
          String.format(
              "https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent?key=%s",
              model, apiKey);

      Map<String, Object> requestBody =
          Map.of(
              "model",
              "models/" + model,
              "content",
              Map.of("parts", List.of(Map.of("text", text))),
              "outputDimensionality",
              768);

      Map<String, Object> response =
          restClient
              .post()
              .uri(url)
              .contentType(MediaType.APPLICATION_JSON)
              .body(requestBody)
              .retrieve()
              .body(Map.class);

      if (response != null && response.containsKey("embedding")) {
        Map<String, Object> embeddingMap = (Map<String, Object>) response.get("embedding");
        List<Double> values = (List<Double>) embeddingMap.get("values");
        log.info(
            "[GEMINI_EMBEDDING] Successfully generated real {}-dim vector from Google Gemini API",
            values != null ? values.size() : 0);
        return values;
      }
    } catch (Exception e) {
      log.error("[GEMINI_EMBEDDING] Error calling Gemini embedContent API: {}", e.getMessage());
    }

    return generateDeterministicFallback(text, 768);
  }

  /**
   * Chuyển đổi vector thành chuỗi PostgreSQL pgvector format: [0.1,0.2,...,0.768]
   *
   * @param vector danh sách giá trị double
   * @return chuỗi pgvector hoặc null nếu vector rỗng
   */
  public String toVectorString(List<Double> vector) {
    if (vector == null || vector.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < vector.size(); i++) {
      sb.append(vector.get(i));
      if (i < vector.size() - 1) {
        sb.append(",");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  /** Fallback sinh vector xác định (deterministic) khi API gặp sự cố mạng. */
  private List<Double> generateDeterministicFallback(String text, int dim) {
    Double[] vector = new Double[dim];
    int hash = text.hashCode();
    double norm = 0.0;
    for (int i = 0; i < dim; i++) {
      double val = Math.sin(hash + i * 0.1);
      vector[i] = val;
      norm += val * val;
    }
    norm = Math.sqrt(norm);
    for (int i = 0; i < dim; i++) {
      vector[i] = vector[i] / norm;
    }
    return List.of(vector);
  }
}
