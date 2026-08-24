package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.response.PropertySemanticResponse;
import com.project.estate.service.PropertyAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/ai/properties")
@RequiredArgsConstructor
@Tag(
    name = "AI Semantic & Hybrid Search",
    description = "Endpoints for pgvector AI Search and Embeddings")
public class PropertyAiController {

  private final PropertyAiService propertyAiService;

  @PostMapping("/sync-embeddings")
  @Operation(summary = "Batch generate 768-dim embeddings for all existing properties in DB")
  public ApiResponse<String> syncEmbeddings() {
    int count = propertyAiService.syncAllPropertyEmbeddings();
    return ApiResponse.success(
        "Successfully generated AI embeddings for " + count + " properties.");
  }

  @GetMapping("/semantic-search")
  @Operation(summary = "Pure semantic natural language search using pgvector HNSW Index")
  public ApiResponse<List<PropertySemanticResponse>> searchSemantic(
      @RequestParam("query") String query,
      @RequestParam(value = "limit", defaultValue = "5") int limit) {
    return ApiResponse.success(propertyAiService.searchSemantic(query, limit));
  }

  @GetMapping("/hybrid-search")
  @Operation(
      summary = "Hybrid Search: Combines SQL Filters (City, Price) + AI Vector Cosine Similarity")
  public ApiResponse<List<PropertySemanticResponse>> searchHybrid(
      @RequestParam("query") String query,
      @RequestParam(value = "city", required = false) String city,
      @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
      @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
      @RequestParam(value = "limit", defaultValue = "5") int limit) {
    return ApiResponse.success(
        propertyAiService.searchHybrid(query, city, minPrice, maxPrice, limit));
  }
}
