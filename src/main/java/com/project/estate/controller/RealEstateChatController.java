package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.response.RealEstateAdvisorResponse;
import com.project.estate.service.RealEstateRagAdvisorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/ai/advisor")
@RequiredArgsConstructor
@Tag(
    name = "AI Real Estate RAG Advisor",
    description = "Endpoints for AI Real Estate Consultant Chatbot")
public class RealEstateChatController {

  private final RealEstateRagAdvisorService realEstateRagAdvisorService;

  @GetMapping("/chat")
  @Operation(summary = "Ask the AI Real Estate Advisor (GET)")
  public ApiResponse<RealEstateAdvisorResponse> chatGet(@RequestParam("query") String query) {
    RealEstateAdvisorResponse response = realEstateRagAdvisorService.advise(query);
    return ApiResponse.success(response);
  }

  @PostMapping("/chat")
  @Operation(summary = "Ask the AI Real Estate Advisor (POST)")
  public ApiResponse<RealEstateAdvisorResponse> chatPost(@RequestBody ChatRequest request) {
    RealEstateAdvisorResponse response = realEstateRagAdvisorService.advise(request.query());
    return ApiResponse.success(response);
  }

  public record ChatRequest(String query) {}
}
